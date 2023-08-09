package com.ytrue.netty.buffer;

/**
 * @author ytrue
 * @date 2023-08-06 15:22
 * @description PoolSubpage核心类，这个类的对象就是用来包装8KB内存的
 */
public class PoolSubpage<T> implements PoolSubpageMetric {

    //该8KB内存是从哪个PoolChunk中分配的
    final PoolChunk<T> chunk;

    //分配的这块8KB内存在Chunk二叉树数组中的下标索引
    private final int memoryMapIdx;

    //这个PoolSubpage包装的8KB内存在Chunk中的内存偏移量的大小
    private final int runOffset;

    //这个PoolSubpage的大小，也就是8KB
    private final int pageSize;

    //一个PoolSubpage是8KB，最多可以分配成512个16B，所以数组容量最大为
    //512，而一个long类型有64位，8个long就有512位，所以用容量为8的long数组就可以有512个索引，也就可以表示PoolSubpage数组中
    //哪个位置使用了，哪个位置没有使用，用0和1来区分。这个就是位图
    private final long[] bitmap;

    //大家应该还记得，在PoolArena中有tiny和small的PoolSubpage数组，这个数组中每一个位置存储的都是
    //PoolSubpage链表，所以在这里应该也能明白，从Chunk中分配出的每一个8KB大小的内存，最终都是要加入到
    //PoolArena的PoolSubpage数组的对应下标的双向链表中的
    //这就是前驱节点指针
    PoolSubpage<T> prev;

    //下一个节点的指针
    PoolSubpage<T> next;

    //是否被销毁
    boolean doNotDestroy;

    //这个属性对应的其实就是用户申请分配的小额内存，经过规整之后的内存大小，因为PoolSubpage会被这个内存大小均分
    //比如8KB的内存可以分成两个4KB，也可以分成512个16B，用户申请的是多少的内存，就按规整后的内存把PoolSubpage均分
    //加入到PoolArena的时候，也是根据均分之后的elemSize大小，来寻找PoolSubpage数组中对应位置的双向链表加入
    int elemSize;

    //该PoolSubpage一共分割成了多少个elemSize
    private int maxNumElems;

    //bitmap数组的容量大小
    //这里为什么要有这个属性呢？因为每一个PoolSubpage可能是按照不同的elemSize均分的，所以分成的内存块的数目也不相同
    //所以位图的长度也就不同，如果是按照16B分割的，那位图数组的长度就是8，因为要表示512个16B
    private int bitmapLength;

    //下一个可以使用的内存块在位图中的位置
    private int nextAvail;

    //该PoolSubpage还剩下的可以分配的内存块数量
    private int numAvail;


    PoolSubpage(int pageSize) {
        chunk = null;
        memoryMapIdx = -1;
        runOffset = -1;
        elemSize = -1;
        this.pageSize = pageSize;
        bitmap = null;
    }


    /**
     * 特殊的构造器方法，创建PoolArena中PoolSubpage数组中存储的双向链表的头节点的方法
     *
     * @param head         头
     * @param chunk        那个chunk
     * @param memoryMapIdx 分配的这块8KB内存在Chunk二叉树数组中的下标索引
     * @param runOffset    这个PoolSubpage包装的8KB内存在Chunk中的内存偏移量的大小
     * @param pageSize     8K
     * @param elemSize     是经过规整后的内存大小
     */
    PoolSubpage(PoolSubpage<T> head, PoolChunk<T> chunk, int memoryMapIdx, int runOffset, int pageSize, int elemSize) {
        this.chunk = chunk;
        this.memoryMapIdx = memoryMapIdx;
        this.runOffset = runOffset;
        this.pageSize = pageSize;
        //pageSize的值为8KB            0000 0000 0000 0000 0010 0000 0000 0000
        //右移10位，实际上就是除以1024    0000 0000 0000 0000 0000 0000 0000 1000
        //值为8，表示bitmap是一个容量为8的long型数组 ，这里的值等于8
        bitmap = new long[pageSize >>> 10];
        //进一步初始化该PoolSubpage
        init(head, elemSize);
    }


    /**
     * 初始化PoolSubpage的方法
     *
     * @param head
     * @param elemSize
     */
    void init(PoolSubpage<T> head, int elemSize) {
        doNotDestroy = true;
        //按照固定的内存大小均分PoolSubpage
        this.elemSize = elemSize;

        if (elemSize != 0) {
            //根据elemSize计算出这个PoolSubpage可以被分成多少块
            //就是让8KB除以elemSize
            //这里把还剩下可以分配的内存块也赋值了
            // maxNumElems 该PoolSubpage一共分割成了多少个elemSize
            // numAvail 该PoolSubpage还剩下的可以分配的内存块数量
            // 如果是512B 可以分为 16块
            maxNumElems = numAvail = pageSize / elemSize;

            // 下一个可以使用的内存块在位图中的位置
            nextAvail = 0;

            // 右移6位就是除以64，一个long是64位
            // 所以这里就是计算的分成的这些内存块需要多少个long来表示
            // 算出来的这个值，就是bitmap数组要初始化的长度
            bitmapLength = maxNumElems >>> 6;

            //这里之所这么做，是考虑到被均分之后的PoolSubpage的内存块数不够64块，如果是这样的话
            //maxNumElems >>> 6得到的就是0，所以下面要加1，这样就意味着会占用一个long来表示这些内存块是否使用了
            if ((maxNumElems & 63) != 0) {
                bitmapLength++;
            }

            //在这里真正初始化了bitmap数组，一开始所有位置都是0，表示内存都还没有被使用
            for (int i = 0; i < bitmapLength; i++) {
                bitmap[i] = 0;
            }
        }
        //在这里把该PoolSubpage添加到PoolArena中PoolSubpage数组相应的双向链表中了
        addToPool(head);
    }


    /**
     * 分配内存的方法，返回的就是位图中的一个索引下标
     * 为什么这个方法不需要参数，之前已经解释过了
     * 该方法的返回值是一个long类型的数，这个数的高32位中的低6位，表示的是当前申请的内存在bitmap的long型数组中其中一个long的哪个
     * 位置，说白了就是一个数组的下标。而高32位的第7到9位表示的则是该内存块使用的是long型数组中的哪个long来表示的
     * 两个数据计算一下就可以得到该内存块在long型数组中的偏移量了
     * 在这里为什么是低6位呢？因为2的6次方正好等于64，恰好可以表示一个long的容量
     * 而7到9位的差值是3，2的3次方正好等于8，long型数组的最大容量也正是8，包含了8个long整数
     *
     * @return
     */
    long allocate() {
        //如果elemSize为0，内存就分配不了呀
        //一个8KB的内存怎么被0均分呀
        if (elemSize == 0) {
            return toHandle(0);
        }

        //剩下的可以被分配的内存块为0，或者该PoolSubpage被销毁则无法分配内存，
        //直接返回-1即可
        if (numAvail == 0 || !doNotDestroy) {
            return -1;
        }

        //在这里就会去找到一个可以分配出去的内存块
        //返回的就是可分配的内存块在位图数组中的下标索引
        // bitmap[] 中找到可以分配的位置，以为一个long 代表64位，8个64 = 512 就可以装下所有
        final int bitmapIdx = getNextAvail();

        //这里得到的就是可以分配出去的内存块在bitmap数组中的索引位置，这个索引位置指的是0到7的这个位图数组的大索引
        //右移6得到的就是高26为呀
        int q = bitmapIdx >>> 6;

        //这里得到的就是低6位
        int r = bitmapIdx & 63;

        //这里是断言该内存块在位图数组中还为分配，因为做与运算为0，这个逻辑之前讲过了
        assert (bitmap[q] >>> r & 1) == 0;

        //这里是把位图数组相应的位置置为1，标志着内存已分配了
        bitmap[q] |= 1L << r;

        //numAvail代表PoolSubpage还剩下的可以分配的内存块数量
        //这里先做了一个减减，因为要分配出去一块内存了
        //然后判断该值是否为0，如果等于0就意味着该PoolSubpage中没有可分配的内存了
        if (--numAvail == 0) {
            //没有可分配的内存了，就要把这个PoolSubpage从PoolArena的PoolSubpage数组中对应的双向链表中删除
            removeFromPool();
        }

        //把这个算出来的在位图数组中的数组下标转换成一个具体的内存句柄
        //就是通过这个值既可以得到8KB内存在Chunk块的二叉树数组中的索引位置，也能得到在PoolSubpage的位图数组中的具体位置
        return toHandle(bitmapIdx);
    }


    /**
     * 释放内存的方法
     *
     * @param head
     * @param bitmapIdx
     * @return
     */
    boolean free(PoolSubpage<T> head, int bitmapIdx) {
        //elemSize为0就直接退出
        if (elemSize == 0) {
            return true;
        }

        //这里的逻辑是不是很熟悉？右移6位得到高26位
        //得到的就是long整数在long数组的索引位置
        int q = bitmapIdx >>> 6;
        //这里得到的就是低6位的值，也就是long整数中的位索引
        int r = bitmapIdx & 63;
        //断言这个位置已经被分配了
        assert (bitmap[q] >>> r & 1) != 0;
        //把它重新设置为为分配状态，也就是设置为0
        bitmap[q] ^= 1L << r;

        //这个位置设置成下一个可以使用的内存块在位图中的位置
        //如果用户申请了同样大小的内存，这个位置就会被直接分配出去，免得再查找一遍了
        setNextAvail(bitmapIdx);

        //注意，下面这里是先取值后加加，这就意味着当释放了一块内存后，整个PoolSubpage中只有被释放的这一块内存可以分配
        //这时候就把这个PoolSubpage放到对应双向链表的头节点后面，将来分配内存时候先从它内部分配
        if (numAvail++ == 0) {
            addToPool(head);
            return true;
        }

        //判断一下可分配的内存块是否等于均分后的内存块数量
        if (numAvail != maxNumElems) {
            //不等于则说明现在的PoolSubpage还有内存被用户使用着
            return true;
        } else {
            //走到这里就是PoolSubpage已经没有内存块被用户使用了，但是该PoolSubpage双向链表中
            //只有这一个PoolSubpage节点，所以还不能从双向链表中移除
            if (prev == next) {
                return true;
            }
            //走到这里说明双向链表中不止有一个PoolSubpage节点，所以直接就把不参与内存分配的PoolSubpage节点移除
            //这里这么做应该只是为了控制双向链表的容量。
            doNotDestroy = false;
            removeFromPool();
            return false;
        }
    }

    private void setNextAvail(int bitmapIdx) {
        nextAvail = bitmapIdx;
    }

    /**
     * 把这个算出来的在位图数组中的数组下标转换成一个具体的内存句柄
     *
     * @param bitmapIdx
     * @return
     */
    private long toHandle(int bitmapIdx) {
        //0x4000000000000000十六进制转换为二进制为  0100 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000
        //bitmapIdx << 32得到的就是一个低32位0，高32位为bitmapIdx的值，做或运算就把bitmapIdx放在long的前32位了
        //然后再和memoryMapIdx做或运算，这时候低32位还是0，做或运算就是memoryMapIdx本身，就相当于把memoryMapIdx放到long的低32位了
        //这里之所以在或上一个0x4000000000000000L，只是不想让数组索引的高位为0，因为第一次分配小额内存的话，long整数的高32为一定是0
        //这样就会使PoolChunk中的allocateRun和allocateSubpage方法得到的结果混淆了。
        return 0x4000000000000000L | (long) bitmapIdx << 32 | memoryMapIdx;
    }

    /**
     * 找到一个可以分配出去的内存块
     *
     * @return
     */
    private int getNextAvail() {
        //第一次查找的时候，nextAvail的值为0，因为它的初始值就是0
        //并且，只要分配过一次内存，该值就会改为-1，并且一直都是-1了。
        //也就是说只要不是第一次分配内存，nextAvail就是-1。用这个标志可以判断该PoolSubpage是否被分配过
        int nextAvail = this.nextAvail;
        //如果该值大于等于0，说明是第一次分配内存
        if (nextAvail >= 0) {
            //把nextAvail设为-1，说明之后就不是第一次分配内存了
            this.nextAvail = -1;
            //直接把0返回出去，因为这时候还是第一次分配内存，long型数组内的值都是0，直接把0号索引返回出去即可
            return nextAvail;
        }
        //走到这里说明不是第一次分配内存了，要真正寻找下一个可以分配的内存块在数组中的位置
        return findNextAvail();
    }

    /**
     * 真正寻找下一个可以分配的内存块在数组中的位置的方法
     *
     * @return
     */
    private int findNextAvail() {
        //得到位图数组
        final long[] bitmap = this.bitmap;
        //得到位图数组的真正长度，这个长度就是均分之后的内存块占用的长度
        final int bitmapLength = this.bitmapLength;
        //首先先遍历整个bitmap位图数组
        for (int i = 0; i < bitmapLength; i++) {
            //得到每一个long整数
            long bits = bitmap[i];
            //这里的运算方式很有意思，就是得到了这个long整数，将它取反
            //比如得到long整数本身为1111 1111 1111 1111 1111 1111 1111 1111 1111 1111 1111 1111 1111 1111 1111 1111
            //这个数值取反就全是0，如果是这样就意味着已经没有可以使用的内存块了，只有不等于0，才有内存可以继续分配
            //所以这个方法就是快速计算当前PoolSubpage是否还有可以被分配的内存
            if (~bits != 0) {
                //如果有就在这里继续分配
                //这里传进去的i就是在位图数组中的第几个long整数中可以分配内存
                //bits就是那个long整数
                return findNextAvail0(i, bits);
            }
        }
        //没有则返回-1，说明内存分配不足
        return -1;
    }

    /**
     * 从bitmap位图数组中找到可以分配出去的内存的方法
     * i就是在位图数组中的第几个long整数中可以分配内存
     * bits就是哪个long整数
     *
     * @param i
     * @param bits
     * @return
     */
    private int findNextAvail0(int i, long bits) {
        //先得到PoolSubpage被均分的块数，这个块数其实就代表了位图数组的有效长度
        final int maxNumElems = this.maxNumElems;
        //现在i已经确定了，就是要分配的内存所占的long整形在long数组中的索引位置
        //将i右移6，得到的就是高26的值，低6位都是0
        final int baseVal = i << 6;
        //现在就是具体查找可以分配出去内存块的逻辑了
        //这里开始遍历我们已经确定的long整数，这时候就是从低位到高位遍历了
        for (int j = 0; j < 64; j++) {
            //这里我们仍然是举一个具体的例子来学习运算逻辑
            //假如bits的位数是这种情况                         0010 0100 0000 0000 0000 0000 0000 0000 0010 0100 0000 0000 0000 0000 0000 1111
            //如果是第一次进行与运算，从最低位开始查找，和1做与运算 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0000 0001
            //得到的值肯定不是0，因为低四位都为1
            //所以不会走下面的分支，会直接走到 bits >>>= 1代码，把bits右移一位，然后再次和1做与运算，当右移了4次之后
            //bits的最低位就为0了，这时候做与运算也就等于0了，找到了可以分配内存的位置，进入下面的分支就行
            if ((bits & 1) == 0) {
                //在这里做了一个或运算，也就是加法运算，把原来的baseVal加上刚才右移的偏移量
                //baseVal是一个低6位都为0的值，这里加上j，低6位就有值了
                //并且表示的就是具体的要分配的内存在long整形中的位数索引
                //而高26位表示的就是要分配的内存所占的long整形在long数组中的索引位置
                //一个数表示了两个值，很简单也很完美
                int val = baseVal | j;
                //再次判断一下，在数组中找到的索引，是不能超过PoolSubpage被均分之后的块数的
                if (val < maxNumElems) {
                    //把该下标索引返回出去
                    return val;
                } else {
                    break;
                }
            }
            //走到这里说明还没有找到可分配的内存，将bits右移1位继续查找
            bits >>>= 1;
        }
        //走到这里说明没有足够的内存可以分配，返回-1即可
        return -1;
    }

    /**
     * 添加到PoolArena中PoolSubpage数组相应的双向链表中的方法
     * 添加到头节点之后，使用的是头插法
     *
     * @param head
     */

    private void addToPool(PoolSubpage<T> head) {
        assert prev == null && next == null;
        prev = head;
        next = head.next;
        next.prev = this;
        head.next = this;
    }


    /**
     * 把该PoolSubpage从PoolArena中PoolSubpage数组相应的双向链表中删除的方法
     */
    private void removeFromPool() {
        assert prev != null && next != null;
        prev.next = next;
        next.prev = prev;
        next = null;
        prev = null;
    }

    @Override
    public String toString() {
        final boolean doNotDestroy;
        final int maxNumElems;
        final int numAvail;
        final int elemSize;
        if (chunk == null) {
            doNotDestroy = true;
            maxNumElems = 0;
            numAvail = 0;
            elemSize = -1;
        } else {
            synchronized (chunk.arena) {
                if (!this.doNotDestroy) {
                    doNotDestroy = false;
                    maxNumElems = numAvail = elemSize = -1;
                } else {
                    doNotDestroy = true;
                    maxNumElems = this.maxNumElems;
                    numAvail = this.numAvail;
                    elemSize = this.elemSize;
                }
            }
        }

        if (!doNotDestroy) {
            return "(" + memoryMapIdx + ": not in use)";
        }

        return "(" + memoryMapIdx + ": " + (maxNumElems - numAvail) + '/' + maxNumElems +
                ", offset: " + runOffset + ", length: " + pageSize + ", elemSize: " + elemSize + ')';
    }


    @Override
    public int maxNumElements() {
        if (chunk == null) {
            // It's the head.
            return 0;
        }

        synchronized (chunk.arena) {
            return maxNumElems;
        }
    }

    @Override
    public int numAvailable() {
        if (chunk == null) {
            // It's the head.
            return 0;
        }

        synchronized (chunk.arena) {
            return numAvail;
        }
    }

    @Override
    public int elementSize() {
        if (chunk == null) {
            // It's the head.
            return -1;
        }

        synchronized (chunk.arena) {
            return elemSize;
        }
    }

    @Override
    public int pageSize() {
        return pageSize;
    }


    void destroy() {
        if (chunk != null) {
            chunk.destroy();
        }
    }
}
