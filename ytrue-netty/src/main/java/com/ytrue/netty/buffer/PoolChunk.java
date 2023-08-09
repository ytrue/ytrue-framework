package com.ytrue.netty.buffer;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author ytrue
 * @date 2023-08-06 11:49
 * @description 这个类可以说是内存分配最核心最核心的类，重要的逻辑都在这个里面，其次就是PoolSubpage这个类
 */
final class PoolChunk<T> implements PoolChunkMetric {


    private static final int INTEGER_SIZE_MINUS_ONE = Integer.SIZE - 1;


    //该PoolChunk属于哪个PoolArena，注意我们之前创建的PoolChunkList也会判断是属于哪个PoolArena的
    //PoolArena中会持有PoolChunkList和PoolSubPage数组，并且PoolArena会被多个线程持有
    //而PoolArena的创建个数和CPU的核数有直接关系，这个大家要记住
    final PoolArena<T> arena;


    //如果我们使用的是直接内存，那这块内存就是ByteBuffer，而ByteBufer又是Unsafe类为我们直接分配的内存
    //如果我们使用的是堆内存，这个属性就是一个byte数组。堆内存的具体逻辑可以去源码中查看，这里我们就不引入了
    final T memory;


    //判断当前的内存块是否可以被池化管理
    final boolean unpooled;

    //这个也是个内存偏移量，默认值为0
    final int offset;


    //这个是个核心二叉树数组，就是用数组这种结构模拟出来的二叉树结构
    //这个数组初始化好之后是会随着内存分配，节点的值也会一直变化
    //而这个数组的每一个值就代表了二叉树的每一个节点的内存的使用情况。数组的容量为4096。0号位置和1号位置都为0。0还位置和二叉树无关，只是占位。
    //但是1号位置就是二叉树的顶点，1号为的值为0。代表二叉数的顶点是二叉树的第0层
    //二叉树的第1层为两个8MB的节点，对应着数组的第2和第3索引位置，这两个索引位置的值都为1，代表着这两个节点在二叉树的第1层。下面的依此类推
    //直到数组的第2048到4095号位置，这些索引位置的值都为11，代表这些叶子节点都在二叉树的第11层。
    //每个节点都是8KB，所以整个二叉树为11层，由16MB平均分配成了2048个8KB的内存。而我们分配内存如果分配的是小额内存
    //其实都是从这些叶子结点中分配的，这些叶子结点都会被PoolSubPage包装，后面就会讲到
    // 0层                   16MB             数组1号索引位置
    // 1层             8MB           8MB      分别为数组2号和3号索引位置
    // 2层           4MB 4MB       4MB  4MB
    //             ........................
    // 11层       8KB 8KB 8KB 8KB 8KB 8KB ........ 2048个
    //当内存还未从这块Chunk中分配走的时候，每个节点在数组中存储的是它位于二叉树中的层数的值，比如11层的叶子节点在数组中存储的值都是11
    //但是一旦有内存开始分配了，会先计算出要分配的内存在二叉树的第几层，然后开始从左向右依次寻找可以被分配出去的内存。并且有一个节点的内存被
    //全部分配出去了，就会把这个节点在数组中存储的值改成12。举个例子
    //现在要分配一块内存，内存经过规整后为8KB大小，那就会先计算出要分配的内存可以从二叉树的第11层分配，所以直接到第11层的叶子结点
    //从第一个叶子结点把内存地址返回出去，因为是第一个叶子结点，也就意味着在Chunk这个内存块中的偏移量
    //为0，直接返回首地址和可用的内存字节长度即可。然后把第一个叶子结点在数组中存储的值改为12，代表这一块的内存分配完了，然后把第一个和第二个
    //叶子结点的父节点的值从10改成11，依此类推，把第一个叶子结点的所有父节点的值都改一下，这是因为内存已经分配出去一点了，所以状态值
    //要改变一点。如果这时候再来分配一个8KB的内存，就从11层找到第二个叶子节点，这个节点在第11层中的偏移量为1，而1个叶子结点就代表8KB，所以
    //在Chunk内存块中的偏移量为8KB，就把这个地址返回就行了。然后把第二个叶子节点在数组中的值改为12。然后这两个叶子结点的父节点的值也可以改为
    //12了，代表这16KB已经分配完了。
    //如果一个Chunk内存块的内存都分配完了，这个Chunk的顶点就会被改为12。
    private final byte[] memoryMap;


    //这个数组和上面那个数组的结构和初始化后的值都一样，只不过这个数组初始化完毕后就不会再改变了。
    //就是用这个数组来做对比，就能通过要申请的内存在该数组中的索引位置，找到要分配的内存在二叉树中的层数
    private final byte[] depthMap;


    //终于到了这个PoolSubpage了，上面我们就说了，Chunk内存块的每一个叶子节点的大小都是8KB，这8KB就可以被一个PoolSubpage包装
    //而这个PoolSubpage数组的容量就是2048。其实就是Chunk每分配出一个叶子结点大小的内存，就会使用一个PoolSubpage来包装一下
    private final PoolSubpage<T>[] subpages;


    //这个值在构造函数中被赋值了，值为-8192 转换成2进制的值为
    //               1111 1111 1111 1111 1110 0000 0000 0000  可以看到，该值的后12位都为0。
    //而8192的二进制为 0000 0000 0000 0000 0010 0000 0000 0000
    //所以如果用户申请的内存和该值做与运算，如果申请的内存是小于8KB的，那这个值的二进制的第13为一定不会为1
    //与运算之后的值一定为0，如果为0就可以断定用户申请的内存小于8KB，就可以优先从PoolSubpage中分配
    //如果与运算后的值不为0了，则说明用户申请的内存大于8KB，就不能从PoolSubpage中分配了，因为PoolSubpage代表的是叶子结点的大小
    //叶子结点的的最大值为8KB
    private final int subpageOverflowMask;

    //其实就是PoolSubpage的大小，为8KB，也就是8192
    private final int pageSize;


    //这个属性就是用来快速计算用户申请的内存经过规整之后要从二叉树的哪一层开始分配
    //具体的计算公式为 int d = maxOrder - (log2(normCapacity) - pageShifts)
    //这个属性的默认值为13，其中maxOrder代表二叉树的树高
    //自己举个例子就大概明白了
    //比如用户现在要申请8KB的内存
    //log(8192) = 13，13减去13为0。然后树高11减去0为11
    //说明申请8KB的内从要从二叉树的第11层叶子节点分配
    private final int pageShifts;


    //树高，值为11
    private final int maxOrder;


    //Chunk内存块的大小，值为16MB
    private final int chunkSize;


    //log2(chunkSize)的值会赋值给该值，值为24
    private final int log2ChunkSize;


    //PoolSubPage数组的初始化容量
    private final int maxSubpageAllocs;


    //这个算是一个状态值，如果二叉树中的某个节点代表的内存已经被分配了
    //就把该节点用这个属性赋值，这个属性在构造器中会被赋值为12
    private final byte unusable;


    //我们上面说了，如果使用的是直接内存，会创建ByteBuffer，而ByteBuffer底层也是用Unsafe来分配的内存
    //所以这里会对创建的ByteBuffer用队列做一个缓存
    //这里缓存的ByteBuffer是被ByteBuf中用到的
    private final Deque<ByteBuffer> cachedNioBuffers;

    //当前Chunk还可以分配的字节数量
    private int freeBytes;

    //该Chunk属于哪个PoolChunkList
    PoolChunkList<T> parent;


    //该Chunk内存块在PoolChunkList中的前驱节点
    PoolChunk<T> prev;
    //该Chunk内存块在PoolChunkList中的下一个节点
    PoolChunk<T> next;


    PoolChunk(PoolArena<T> arena, T memory, int pageSize, int maxOrder, int pageShifts, int chunkSize, int offset) {
        //这个就是决定是否要用池化
        //设置成false意思就是不用非池化的
        unpooled = false;

        //把所属的Poolarena赋值过来
        this.arena = arena;

        //memory就是该Chunk所拥有的16MB内存，真正的分配方法大家可以去PoolArena中看一下，你会在PoolArena的内部类DirectArena
        //中的newChunk方法内部看见new PoolChunk()方法。该构造方法中有一个allocateDirect方法，就是该方法分配了一块直接内存
        //底层就是unsafe.allocateMemory()分配的内存，这里其实就是Unsafe分配了一块内存交给了ByteBuffer，然后把这个ByteBuffer返回了
        //又交给了Chunk中的memory
        this.memory = memory;

        //每个叶子节点的大小，默认为8KB
        this.pageSize = pageSize;

        //值为13，作用在上面的成员变量讲解时已经说了
        this.pageShifts = pageShifts;

        //二叉树的最大的层数，为11
        this.maxOrder = maxOrder;

        //chunk内存块为16MB
        this.chunkSize = chunkSize;

        //值为0
        this.offset = offset;

        //作用也讲解过了，值为12
        unusable = (byte) (maxOrder + 1);

        // log2(chunkSize)的值会赋值给该值，值为24
        log2ChunkSize = log2(chunkSize);

        //掩码，作用上面也已经讲过了，就不再重复了
        subpageOverflowMask = ~(pageSize - 1);

        //该chunk内存块还可以分配的内存值，初始值为16MB
        freeBytes = chunkSize;

        assert maxOrder < 30 : "maxOrder should be < 30, but is: " + maxOrder;
        //1的二进制左移11位
        //0000 0000 0000 0000 0000 1000 0000 0000
        //这个值为2048，代表了该Chunk块可以分配的最多的Subpage的个数
        maxSubpageAllocs = 1 << maxOrder;

        //下面就是具体初始化memoryMap的逻辑了
        //字节数组的容量为4096
        memoryMap = new byte[maxSubpageAllocs << 1];

        //这个数组初始化完成后内部的值就不会再改变了
        depthMap = new byte[memoryMap.length];

        int memoryMapIndex = 1;
        //这里看着代码有点绕，但是自己从头代入几个数就全清楚了，很简单的，所以就不写更多注释了
        for (int d = 0; d <= maxOrder; ++d) {
            int depth = 1 << d;
            for (int p = 0; p < depth; ++p) {
                memoryMap[memoryMapIndex] = (byte) d;
                depthMap[memoryMapIndex] = (byte) d;
                memoryMapIndex++;
            }
        }
        //初始化subpages数组
        subpages = newSubpageArray(maxSubpageAllocs);
        //初始化队列
        cachedNioBuffers = new ArrayDeque<ByteBuffer>(8);
    }


    /**
     * 非池化的构造器，不是我们的重点内容，所以就略过了
     *
     * @param arena
     * @param memory
     * @param size
     * @param offset
     */
    PoolChunk(PoolArena<T> arena, T memory, int size, int offset) {
        unpooled = true;
        this.arena = arena;
        this.memory = memory;
        this.offset = offset;
        memoryMap = null;
        depthMap = null;
        subpages = null;
        subpageOverflowMask = 0;
        pageSize = 0;
        pageShifts = 0;
        maxOrder = 0;
        unusable = (byte) (maxOrder + 1);
        chunkSize = size;
        log2ChunkSize = log2(chunkSize);
        maxSubpageAllocs = 0;
        cachedNioBuffers = null;
    }

    /**
     * 分配内存的方法，这里的参数buf，还记得我们上面说的吧，分配的内存会被包装在ByteBuf中，也就是PooledByteBuf
     * 类型的对象中。这个对象是从对象池中获得的。
     * reqCapacity使用户申请的内存
     * normCapacity是经过规整后的内存大小
     *
     * @param buf
     * @param reqCapacity
     * @param normCapacity
     * @return
     */
    boolean allocate(PooledByteBuf<T> buf, int reqCapacity, int normCapacity) {
        //handle其实就可以看成一个内存偏移量，是一个long类型的，说明有64位。
        //该数值的低32位代表了所获的内存块在Chunk内存块中的内存偏移量，其实就是在Chunk内存块的二叉树数组中的索引位置，索引位置不就是相对于
        //数组起始地址的偏移量吗？
        //而该数值的高32位则代表了内存是从PoolSubpage数组中分配的，一个PoolSubpage是8KB，最多可以分配成512个16B，所以数组容量最大为
        //512，而一个long类型有64位，8个long就有512位，所以用容量为8的long数组就可以有512个索引，也就可以表示PoolSubpage数组中
        //哪个位置使用了，哪个位置没有使用，用0和1来区分。这个就是位图，在PoolSubpage中我们会见到的。
        //大家只需要知道此时的handle的高32位的值就是和位图有关即可。
        final long handle;

        //subpageOverflowMask这个属性的作用我们在讲解成员变量的时候详细注释了，这里直接做与运算，如果不等于0，
        //就可以判定用户申请的内存是大于等于8KB的
        //所以我们就要直接去Chunk内存块中分配，如果是小于8KB的，我们才要去PoolSubpage数组中分配
        if ((normCapacity & subpageOverflowMask) != 0) {
            //走到这里说明分配的是大于等于8KB的内存
            //既然是大于等于8KB的，请大家注意。
            //分配出来的是叶子结点，如果申请的正好等于8KB，那就直接分配一个叶子结点大小的内存，并且这个内存不会交给PoolSubpage
            //如果申请的内存大于8KB，比如申请的是16KB，那就会直接从叶子结点的父节点分配一个16KB的内存。
            handle = allocateRun(normCapacity); // 这里返回是是数组的索引
        } else {
            //走到这里说明分配的是小于8KB的内存，既然是小于8KB的内存，那就是从叶子节点中分配
            //实际上也就是从PoolSubpage中分配的小额内存
            //注意啊，如果已经要从Chunk中申请小额内存了，说明内存池和PlloArena中的PoolSubpage数组都已经分配失败了
            //这时候就要先从Chunk内存块中分配一个8KB的PoolSubpage内存，然后再从该PoolSubpage内存中分配小额内存
            //这个PoolSubpage内存也会被加入到PoolArena的PoolSubpage数组中
            handle = allocateSubpage(normCapacity); // 这里返回的是poolsubpage 在bitmap 位置 tohanel
        }


        //走到这里说明从该Chunk中分配内存失败了，所以就会寻找其他的Chunk内存块申请内存
        //所谓内存分配失败，就意味着该Chunk内存块中没有足够的内存可分配出去了。
        if (handle < 0) {
            return false;
        }

        //还记得之前在讲解该类的成员变量时有一个Deque<ByteBuffer> cachedNioBuffers队列吗？
        //这个队列缓存了一些ByteBuffer，这些ByteBuffer是要给PooledDirectByteBuf内部使用的。这里缓存一些其实就是避免频繁创建
        //然后频繁销毁，减少垃圾回收而已
        //如果队列中没有则直接返回null
        ByteBuffer nioBuffer = cachedNioBuffers != null ? cachedNioBuffers.pollLast() : null;

        //初始化Bytebuf，这个Bytebuf就是从方法一开始就传进来的PooledByteBuf
        //这里也在一次印证了分配的内存其实是交给了ByteBuf，或者说是让ByteBuf做了一层包装才能被使用的
        initBuf(buf, nioBuffer, handle, reqCapacity);
        return true;
    }

    /**
     * 分配小于8KB的内存的核心方法，是从PoolSubpage中分配的
     *
     * @param normCapacity
     * @return
     */
    private long allocateSubpage(int normCapacity) {
        // 这里就是真正的从PoolSubpage中分配内存的逻辑了。
        // 首先从PoolArena持有的PoolSubpage数组中寻找合适的分配链表的头节点
        //
        // 存放Tiny内存大小的PoolSubpage数组，容量为32，并且数组中的每个位置存储的都是一个PoolSubpage的双向链表,Tiny的大小为16B到496B，按照16B的大小递增
        // 存放Small内存大小的PoolSubpage数组，容量为4，并且数组中的每个位置存储的都是一个PoolSubpage的双向链表,Small的大小为512B到4KB，按照乘以2的大小递增
        // 判断要申请的内存是否小于512B,是小于512就是 normCapacity / 2 / 2/ 2 / 2 = 获取索引位置之后获取里面的对象
        // 大于512也是差不多的道理，要获取对于的索引位置值，512，1024，2048，4056
        PoolSubpage<T> head = arena.findSubpagePoolHead(normCapacity);

        //得到二叉树的高度
        int d = maxOrder;
        //还不是在每个线程私有的内存池中分配内存，所以要考虑到并发问题，加锁即可解决
        synchronized (head) {
            //从Chunk内存块中具体分配一块等于8KB的内存
            //因为要分配的内存小于8KB，所以肯定要从叶子结点上分配，肯定要从11层中分配，所以这里
            //可以直接把深度11直接传进方法中，这里返回的就是可以分配内存的叶子结点在Chunk内存块的二叉树数组中的索引下标
            //这时候从Chunk中分配的一个8KB的PoolSubpage内存，已经确定好了
            int id = allocateNode(d);
            //如果内存不足则返回-1
            if (id < 0) {
                return id;
            }
            //得到该Chunk对象中的subpages数组，该数组的容量为2048，可以存储2048个大小为8KB的PoolSubpage对象
            //实际上就是把一个Chunk分成了2048个8KB，放到了该数组中
            final PoolSubpage<T>[] subpages = this.subpages;

            //得到PoolSubpage的大小，这里是8k = 1024* 8
            final int pageSize = this.pageSize;

            //要从Chunk内存块中分配出去一个PoolSubpage，所以剩余的可用容量要减去8KB
            freeBytes -= pageSize;

            //现在的id是要分配的8KB的内存在二叉树数组中的索引下标
            //现在要通过subpageIdx方法，根据该下标计算出当前要分配的8KB内存块在subpages数组中的位置
            //返回的同样是一个索引下标
            // id - 2048
            int subpageIdx = subpageIdx(id);

            //根据下标得到subpages对象
            PoolSubpage<T> subpage = subpages[subpageIdx];

            //如果该对象为null，说明从Chunk内存块中分配出来的8KB内存还没有交给PoolSubpage对象包装
            //所以要创建一个PoolSubpage对象
            if (subpage == null) {
                //这里就创建了一个PoolSubpage对象，可以看到在构造器中传进去的参数
                //把head传进去是因为创建的PoolSubpage对象还要加入到PoolArena的PoolSubpage数组对应的链表中
                //runOffset方法计算的就是分配出去的8KB内存在Chunk内存块的memory中的具体偏移量
                //注意啊，之前我们一直持有的是要分配出去的8KB的内存块在Chunk内存块的二叉树数组中的数组下标
                //runOffset方法就是计算具体的偏移量的
                subpage = new PoolSubpage<T>(head, this, id, runOffset(id), pageSize, normCapacity);

                //得到了创建的PoolSubpage对象，先把它放在subpages数组的相应位置
                subpages[subpageIdx] = subpage;
            } else {

                //这里就是Chunk内存中subpages数组中相应位置的subpage不为空的情况
                //那就直接初始化该subpage对象即可
                subpage.init(head, normCapacity);
            }

            //最后从创建的PoolSubpage对象中分配内存
            //为什么这里分配内存不需要用户申请的内存当作参数了？
            //因为用户申请的是小额内存，而每创建一个PoolSubpage对象，都会根据规整过的小额内存，把PoolSubpage均分程相应的块数
            //分配的时候只需从PoolSubpage中根据位图分配出去一块即可
            return subpage.allocate();
        }
    }


    /**
     * 该方法就是从Chunk内存块中分配一个大于8KB的内存
     * 返回的就是一个内存偏移量，实际上是分配的内存块在memoryMap数组中的下标
     * 但同时也是分配的内存在Chunk内存块中的偏移量
     * 而Chunk内存块的首地址是知道的，就是在memory中，使用内存其实使用的就是memory中的内存
     * 到下面大家会看到具体的逻辑
     *
     * @param normCapacity
     * @return
     */
    private long allocateRun(int normCapacity) {
        //下面这行代码的作用在讲解pageShifts成员变量的时候已经说过了
        //就是快速计算用户申请的内存经过规整之后要从二叉树的那一层开始分配
        //大家可以想一想，pageShifts为什么要等于13？因为2的13次方等于8192，所以log2(8192)就等于13
        //所以log2(normCapacity) - pageShifts)就可以写成log2(normCapacity) - log2(8KB)
        //而log2(normCapacity) - log2(8KB)就是log2(normCapacity/8KB)  其实这个转换公式也是我现查的，一眼就能猜到要这么转换，
        //但是初中知识我都忘了，否则我就不会查一下去验证了。。
        //normCapacity/8KB不就是求一下要分配的内存占用了几个叶子节点吗？
        //至于让maxOrder减去这个数，得到的不就是要从第几层开始分配吗？因为第11层可以分配一个叶子结点，第10层一个节点就可以分配两个叶子结点
        //大小的内存，依此类推即可,
        // 这个内存大小是从第几层获取的
        int d = maxOrder - (log2(normCapacity) - pageShifts);

        //这个方法就是详细查找从哪个节点分配内存
        //具体的分配逻辑就在该方法内
        //如果没有分配成功就会返回-1，id就等于-1
        int id = allocateNode(d);
        if (id < 0) {
            //这里返回就是分配失败了
            return id;
        }
        //因为要分配走一块内存了，所以要更新该Chunk内存块可分配的内容值，就是让freeBytes减去要分配出去的内存大小
        freeBytes -= runLength(id);
        return id;
    }


    /**
     * 计算从Chunk内存块中分配出去的内存在Chunk内存块的memory中的具体偏移量
     * 这里传进来的id就是要分配出去内存在Chunk二叉树数组中的下标索引
     * <p>
     * <p>
     * 计算id在 这一层的第几号索引位置，之后乘以 这一层的 内存大小 就可以获得偏移量了
     *
     * @param id
     * @return
     */
    private int runOffset(int id) {
        //depth方法得到得是该数组下标对应在二叉树的第几层
        //如果得到的是第11层，那么将1左移11位得到 0000 0000 0000 0000 0000 1000 0000 0000
        //id假如就是2048 二进制就为            0000  0000 0000 0000 0000 1000 0000 0000
        //位运算得到的就是0
        //说明这个要分配出去的内存在第11层中就是最左子节点，在这一层中没有偏移量。
        //如果id换成2049，那二进制做位运算得到的结果就是1，说明要分配的内存在11层相对于最左子节点的偏移量为1
        // 这里就是计算这个id，在这里层的第几个位置
        int shift = id ^ 1 << depth(id);

        //上面计算的是在相应的层数对照最左节点的偏移量
        //runLength方法计算的就是在相应的层数，每一个节点代表多大的内存
        //有几个偏移量，然后乘以一个偏移量代表的内存大小，是不是就得到了要分配的内存在16MB内存中的具体偏移量了？
        //逻辑就是这样的
        //这里大家一定要弄清楚，Chunk内存块中的二叉树的每一层大小加起来都是16MB，所以不管要分配的节点落在哪一层，都能
        //根据具体的位置算出内存偏移量
        // 这里就是位置 * 这一层是多大大内存 就可以得出它的偏移量了
        return shift * runLength(id);
    }

    /**
     * 计算出当前要分配的8KB内存块在subpages数组中的位置,maxSubpageAllocs的值为2048
     *
     * @param memoryMapIdx
     * @return
     */
    private int subpageIdx(int memoryMapIdx) {
        //^运算就是相同为0，不同为1
        //如果现在传进来的memoryMapIdx是2049
        //二进制就是                   0000 0000 0000 0000 0000 1000 0000 0001
        //maxSubpageAllocs的二进制为   0000 0000 0000 0000 0000 1000 0000 0000
        //位运算的结果为1。说明二叉树数组2049索引位置的数据在subpages数组中的索引为1
        // 比如说 2048 位置就在 0 ，2049 就是 1 ，2050 就是2
        // 可以理解 memoryMapIdx 必须是大于等于2048  - 2048
        return memoryMapIdx ^ maxSubpageAllocs;
    }


    /**
     * 计算的就是在相应的层数，每一个节点代表多大的内存
     *
     * @param id
     * @return
     */
    private int runLength(int id) {
        //大家可以举一个具体例子看看运算的规律和逻辑，这里我就不在举例了
        //log2ChunkSize的值我们也讲过了
        return 1 << log2ChunkSize - depth(id);
    }

    /**
     * 数组下标对应二叉树的第几层
     *
     * @param id
     * @return
     */
    private byte depth(int id) {
        return depthMap[id];
    }


    /**
     * 从Chunk内存块中具体分配一块大于等于8KB的内存的具体方法,参数就是要分配的内存在二叉树的第几层的那个层数
     *
     * @param d
     * @return
     */
    private int allocateNode(int d) {
        //这里初始化的id为1，其实就是先找到二叉树数组memoryMap索引位置为1存储的数据
        //为什么这么做？这时候大家应该都清楚了二叉树数组的索引对应的就是二叉树的每个节点，而存储的值其实就是节点所在的层数
        //二叉树数组的0号位置并不参数分配，所以1号位置得到的就是二叉树的顶级父节点代表的层数。
        //我们也都知道，二叉树数组中的每个元素对应存储的层数值是会随着内存分配而改变的，某个节点上的内存被分配完了，
        //该节点对应的层数就会被置为12。如果顶点被置为12了，说明该Chunk内存块的内存就全被分配完了
        //这里得到1的用意就是为了找到二叉树数组memoryMap索引位置为1存储的数据，然后拿传进来的这个d和二叉树数组中1位置存储的值相比
        //如果父节点的值已经大于传进来的这个数的值了，说明已经没有内存可以分配了，直接退出返回-1即可。
        int id = 1;

        //0假设我们要分配的是一个8KB大小的内存，那么上面传进来的d一定等于11，因为8KB的内存只能从二叉树的叶子节点开始分配
        //按照下面的代码，把1左移11为就得到了一个二进制数
        //0000 0000 0000 0000 0000 1000 0000 0000
        //前面加一个-号，得到的二进制的值就是，得到该值的补码
        //1111 1111 1111 1111 1111 0111 1111 1111
        //在这个值的基础上加1 就得到了该值的补码
        //1111 1111 1111 1111 1111 1000 0000 0000
        // 1 * 2 * 2 xxxx ,如果d= 11 就是 2的11次方
        int initial = -(1 << d);

        //这里就是得到二叉树的根节点在数组中对应的值。这个根节点就是二叉树的顶级父节点
        byte val = value(id);

        //这里就是做对比的逻辑，如果顶级节点对应的值都比d大，说明没有足够的内存可以分配，直接返回-1即可
        if (val > d) {
            return -1;
        }

        //这里就是具体寻找可分配内存的节点的逻辑了，是一个while循环
        //首先，要判断二叉树顶点在数组中的值要小于d，这才意味着该Chunk内存块中还有内存可以分配
        //然后用id这个数组下标和之前那个initial做与运算看是否等于0。这里我们把上面那例子延续到这里，也就是用上面得到的那个补码
        //和id做与运算结果如下
        //现在id是1，还没有改变 二进制为 0000 0000 0000 0000 0000 0000 0000 0001
        //initial的二进制为           1111 1111 1111 1111 1111 1000 0000 0000
        //结果为 0000 0000 0000 0000 0000 0000 0000 0000 结果就是0
        //遇到这种位运算，大家的逻辑切入点应该在哪里？应该从阈值入手，就找到与运算不为0的那个临界值，一般来说，
        //逻辑都可以从那个值中看出来。比如说这个例子，我们发现initial这个值是固定的，如果一个值和它做与运算
        //只有这个值的低12位一旦为1，与运算就不会等于0了。而我们这个例子一开始是用d为11举的例子，
        //如果idinitial做与运算大于0了，说明id的值一定大于等于2048了，也就是已经查找到了第11层的左子节点
        //2048的二进制为              0000 0000 0000 0000 0000 1000 0000 0000
        //initial的二进制为           1111 1111 1111 1111 1111 1000 0000 0000
        //做与运算正好为不为0，如果等于0说明现在还没有查找到第11层
        //所以这里这么搞实际上就是为了保证我们最后找到的这个分配内存的节点，要符合d的要求，换句话说，我们之前算出来d在第几层，这里
        //就应该在第几层，只有到了这个层数，循环才可以被打破
        while (val < d || (id & initial) == 0) {
            //这里把1左移一位，就得到了二进制的2，也就得到了二叉树数组中索引为2的位置的索引
            //每次都左移一位，实际上找到了就是二叉树下一层的左子节点在数组中的下标
            //比如一开始是1，代表的是根节点，然后是2，代表的是根节点的左子节点
            //再左移1位就是4，代表的就是以第二层左子节点为根节点的在第三层的左子节点
            //再左移1位就是8，大家可以画一下图，数组中的位置是八，对应的就是二叉树第4层的左子节点
            //这里也能看出来，在寻找可分配的内存节点时，总是先判断父节点是否容量足够，然后再判断父节点的左子节点
            //如果左子节点容量不够，就去找右子节点。也就是左子节点的兄弟节点。总之，就是这个流程分配的
            id <<= 1;

            //根据索引得到该位置对应的层数，然后用该层数和传进来的d做比较，可以判断该位置的节点内存是否可以被分配
            //这里如果循环了一定的次数，查找到第十一层的最左子节点可以分配8KB的内存，这时候val的值一定等于d，并且id & initial不等于0
            //那么while循环就可以被打破了
            val = value(id);

            //如果该位置节点的层数大于d，说明内存不够分配，那就去得到该节点的兄弟节点，然后再用兄弟节点在数组中对应的层数的值
            //去和d做比较
            //但是我们在递归遍历二叉树的过程中，只有再快找到分配内存的节点时，才会走到这个判断中。
            //大部分情况val和d做比较都是小于的情况
            //因为我们会先从根节点开始比较，然后第二层的比较一次，再从第三层比较一次，最后比较到第11层，如果第11层的第一个左子节点
            //内存不够，val大于d，这时候才会找左子节点的兄弟节点，兄弟节点的内存一定是够的，因为实现比较过父节点的值的
            //父节点够了才会继续向下寻找，所以这时候真正可以分配内存的就是左子节点的兄弟节点，把该节点在二叉树数组中的下标返回出去即可
            if (val > d) {
                //得到兄弟节点，这个位运算我就不举例子了，大家自己算一算就行
                id ^= 1;
                //得到兄弟节点对应的二叉树层值
                val = value(id);
            }
        }

        //这里就找到了可以分配的二叉树节点对应的层数，id就是可以分配内存的节点在二叉树数组中对应的下标索引
        byte value = value(id);

        assert value == d && (id & initial) == 1 << d : String.format("val = %d, id & initial = %d, d = %d", value, id & initial, d);

        //因为该节点的内存分配出去了，所以要把该节点在数组中对应的值设置成不可分配的状态值，
        setValue(id, unusable);

        //递归更新父节点对应的二叉树数组存储的层数值，我们上面讲了，子节点只要有内存分配，都会影响到父节点的状态的
        updateParentsAlloc(id);
        //返回分配出去的节点在二叉树数组中的下标索引
        return id;
    }


    /**
     * 递归更新父节点对应的二叉树数组存储的层数值
     * 这里传进来的参数id就是找到的要分配内存的节点在二叉树数组中的索引。
     *
     * @param id
     */
    private void updateParentsAlloc(int id) {
        //为什么以大于1为打破循环的标志，因为1就是二叉树根节点
        //在数组中的下标，递归更改到根节点，也就是下标为1，根节点搞完了才可以退出循环
        while (id > 1) {
            //右移会使二进制数值变小，对应的找到的数组下标也会变小，正好符合寻找父节点的规律
            //下面的逻辑大家就自己看吧，画画图什么的。我就不细讲了，这就是纯粹的运算问题。
            int parentId = id >>> 1;
            byte val1 = value(id);
            byte val2 = value(id ^ 1);
            //和兄弟节点比较大小，谁的值小，就用谁赋值给父节点。依此类推
            byte val = val1 < val2 ? val1 : val2;
            setValue(parentId, val);
            id = parentId;
        }
    }

    /**
     * 初始化buf的方法
     *
     * @param buf         包装的buf
     * @param nioBuffer   原始的ByteBuffer
     * @param handle      大于等于8k就是 树的索引位置，小于8K 就是subpage的bitmap位置 = toHandle(bitmapIdx)
     * @param reqCapacity 申请的内存大小-没有调整过的
     */
    void initBuf(PooledByteBuf<T> buf, ByteBuffer nioBuffer, long handle, int reqCapacity) {
        //通过handle的高32位和低32位分别取出memoryMapIdx和bitmapIdx
        //我们之前说过，handle是个内存偏移量，实际上就是它的long的64为存储了它在Chunk内存块中的下标
        //和在位图中的具体位置
        //取出在Chunk中的数组下标
        // 这里就是返回一个 （int） handle
        int memoryMapIdx = memoryMapIdx(handle);

        //取出在位图中的位置
        int bitmapIdx = bitmapIdx(handle);

        if (bitmapIdx == 0) {
            //这里是判断了没有使用位图，就意味着是从Chunk中分配的大于等于8KB大小的内存，所以直接初始化Buf即可
            byte val = value(memoryMapIdx);
            assert val == unusable : String.valueOf(val);

            buf.init(this, nioBuffer, handle, runOffset(memoryMapIdx) + offset,
                    reqCapacity, runLength(memoryMapIdx), arena.parent.threadCache());
        } else {
            //这里则意味这有位图，是从PoolSubpage中分配的内存，所以初始化Buf也要用到PoolSubpage才行
            initBufWithSubpage(buf, nioBuffer, handle, bitmapIdx, reqCapacity);
        }
    }

    void initBufWithSubpage(PooledByteBuf<T> buf, ByteBuffer nioBuffer, long handle, int reqCapacity) {
        initBufWithSubpage(buf, nioBuffer, handle, bitmapIdx(handle), reqCapacity);
    }

    private void initBufWithSubpage(PooledByteBuf<T> buf, ByteBuffer nioBuffer,
                                    long handle, int bitmapIdx, int reqCapacity) {
        assert bitmapIdx != 0;

        int memoryMapIdx = memoryMapIdx(handle);

        PoolSubpage<T> subpage = subpages[subpageIdx(memoryMapIdx)];
        assert subpage.doNotDestroy;
        assert reqCapacity <= subpage.elemSize;

        buf.init(
                this, nioBuffer, handle,
                //这里是分配小额内存，小于8KB的内存时，初始化ByteBuf时的计算内存偏移量的方法
                //offset在这里仍然是0
                //0x3FFFFFFF的二进制为   0011 1111 1111 1111 1111 1111 1111 1111
                //还记得我们在PoolSubpage中的toHandle方法中或了一个0x4000000000000000L？
                //这里就是用0x3FFFFFFF来做位运算，得到分配的内存在叶子节点中的具体偏移量，然后乘以内存块的大小，就是相对
                //Chunk内存块具体的内存偏移量了
                //这里我们还是用具体的例子来学习即可
                //比如bitmapIdx 的值就是 0100 0000 0000 0000 0000 0000 0000 0000 这个表示的是在PoolSubpage中0偏移量的内存小块
                //或者使bitmapIdx为  0100 0000 0000 0000 0000 0000 0100 0001  这个就代表着在位图数组中的索引为1，在long整数中位数索引为1
                //大家可以使用这两个数具体算一下内存偏移量，得到的结果是完全精准的
                runOffset(memoryMapIdx) + (bitmapIdx & 0x3FFFFFFF) * subpage.elemSize + offset,
                reqCapacity, subpage.elemSize, arena.parent.threadCache());
    }


    /**
     * 释放内存的方法
     *
     * @param handle
     * @param nioBuffer
     */
    void free(long handle, ByteBuffer nioBuffer) {
        //得到要释放的内存在Chunk内存块的二叉树数组中的下标
        int memoryMapIdx = memoryMapIdx(handle);
        //得到要释放的内存在位图中的索引下标
        int bitmapIdx = bitmapIdx(handle);
        // 如果位图索引不等于0，说明当前内存块是小于8KB的内存块，因而将其释放过程交由PoolSubpage进行
        //如果位图索引下标不等于0，说明要释放的内存块是从PoolSubpage中分配的，一定是小于8KB的内存，所以释放的时候，
        //也应该从PoolSubpage中释放
        if (bitmapIdx != 0) {
            //得到subpages数组中相应下标的那个subpage对象
            PoolSubpage<T> subpage = subpages[subpageIdx(memoryMapIdx)];
            assert subpage != null && subpage.doNotDestroy;
            //从PoolArena中获取PoolSubpage数组中对应的链表头节点
            PoolSubpage<T> head = arena.findSubpagePoolHead(subpage.elemSize);
            synchronized (head) {
                //在这里释放内存了
                if (subpage.free(head, bitmapIdx & 0x3FFFFFFF)) {
                    return;
                }
            }
        }
        //走到这里说明位图为0，说明要释放的内存是个大于8KB的内存，直接从Chunk中释放即可
        //根据数组下标，计算出要释放的内存大小
        freeBytes += runLength(memoryMapIdx);
        //把Chunk内存块的二叉树数组对应下标存储的值重制一下，重置的值就是原来它对应的值
        //比如原来11层的叶子结点对应的就是11，就把它从12置成11。
        setValue(memoryMapIdx, depth(memoryMapIdx));
        //递归更新父节点状态值的方法
        updateParentsFree(memoryMapIdx);
        //这里是把ByteBuffer对象放到cachedNioBuffers队列中缓存起来，下一次使用的时候就可以直接拿了
        //这个ByteBuffer其实是PooledDirectByteBuf中可能会用到的一个成员变量，具体用到的地方
        //大家可以在代码中点一点，看一看。
        if (nioBuffer != null && cachedNioBuffers != null &&
                cachedNioBuffers.size() < PooledByteBufAllocator.DEFAULT_MAX_CACHED_BYTEBUFFERS_PER_CHUNK) {
            cachedNioBuffers.offer(nioBuffer);
        }
    }


    /**
     * 递归更新父节点状态值的方法
     * @param id
     */
    private void updateParentsFree(int id) {
        int logChild = depth(id) + 1;
        while (id > 1) {
            int parentId = id >>> 1;
            byte val1 = value(id);
            byte val2 = value(id ^ 1);
            logChild -= 1;
            if (val1 == logChild && val2 == logChild) {
                setValue(parentId, (byte) (logChild - 1));
            } else {
                byte val = val1 < val2 ? val1 : val2;
                setValue(parentId, val);
            }
            id = parentId;
        }
    }


    /**
     * 得到long的高32位，右移32，就得到高32为了
     * Integer.SIZE正好是32
     *
     * @param handle
     * @return
     */
    private static int bitmapIdx(long handle) {
        return (int) (handle >>> Integer.SIZE);
    }


    private static int memoryMapIdx(long handle) {
        return (int) handle;
    }

    /**
     * 得到该节点处的索引对应在数组中存储的值
     *
     * @param id
     * @return
     */
    private byte value(int id) {
        return memoryMap[id];
    }


    /**
     * 内存分配出去了，要把节点设置成不可分配的状态
     *
     * @param id
     * @param val
     */
    private void setValue(int id, byte val) {
        memoryMap[id] = val;
    }


    /**
     * 计算该Chunk的内存利用率的方法
     *
     * @return
     */
    @Override
    public int usage() {
        final int freeBytes;
        synchronized (arena) {
            freeBytes = this.freeBytes;
        }
        return usage(freeBytes);
    }


    /**
     * 计算该Chunk的内存利用率的方法
     *
     * @param freeBytes
     * @return
     */
    private int usage(int freeBytes) {
        if (freeBytes == 0) {
            return 100;
        }
        int freePercentage = (int) (freeBytes * 100L / chunkSize);
        if (freePercentage == 0) {
            return 99;
        }
        return 100 - freePercentage;
    }


    void destroy() {
        arena.destroyChunk(this);
    }

    @Override
    public int freeBytes() {
        synchronized (arena) {
            return freeBytes;
        }
    }

    @Override
    public int chunkSize() {
        return chunkSize;
    }

    @Override
    public String toString() {
        final int freeBytes;
        synchronized (arena) {
            freeBytes = this.freeBytes;
        }

        return new StringBuilder()
                .append("Chunk(")
                .append(Integer.toHexString(System.identityHashCode(this)))
                .append(": ")
                .append(usage(freeBytes))
                .append("%, ")
                .append(chunkSize - freeBytes)
                .append('/')
                .append(chunkSize)
                .append(')')
                .toString();
    }

    private static int log2(int val) {
        return INTEGER_SIZE_MINUS_ONE - Integer.numberOfLeadingZeros(val);
    }


    private PoolSubpage<T>[] newSubpageArray(int size) {
        return new PoolSubpage[size];
    }
}
