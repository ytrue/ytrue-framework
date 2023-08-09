package com.ytrue.netty.buffer;

import com.ytrue.netty.util.internal.LongCounter;
import com.ytrue.netty.util.internal.PlatformDependent;
import com.ytrue.netty.util.internal.StringUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ytrue.netty.util.internal.ObjectUtil.checkPositiveOrZero;
import static java.lang.Math.max;

/**
 * @author ytrue
 * @date 2023-08-08 10:09
 * @description 内存的分配和回收实际上是在这个类中进行，不过说是在这个类中进行，也只是一个过度，因为 内存的真正分配和释放还是在PoolChunk类中进行的
 * 这里我再跟大家强调一下PoolArena中持有的可分配的内存对象
 * 该类持有Chunk链表，一个tinySubpagePools数组，一个smallSubpagePools数组
 */
abstract class PoolArena<T> implements PoolArenaMetric {

    static final boolean HAS_UNSAFE = PlatformDependent.hasUnsafe();


    enum SizeClass {
        //Tiny的大小为16B到496B，按照16B的大小递增
        Tiny,

        //Small的大小为512B到4KB，按照乘以2的大小递增
        Small,

        //Normal的大小为8KB到16MB，按照乘以2的大小递增，大于16MB的为Huge，不会被缓存，这里的意思是不会被申请为Chunk
        //实际上，在Netty的内存分配中，是先从内存中申请了一大块大内，然后再从这一大块内存中逐渐分配给各个线程使用的
        //这一大块内存就为Chunk，值为16MB
        Normal
    }


    //512右移4实际上就是512除以16，结果为32，这个值就是tinySubpagePools数组的容量
    //因为Tiny是按照16的大小递增的，而它的范围是从16B到496B，递增到496B，一共是31个值，再加上0，就是32个值
    static final int numTinySubpagePools = 512 >>> 4;

    //所属的内存分配器
    final PooledByteBufAllocator parent;


    //这个参数就是二叉树的深度，也可以说是高度
    //Chunk是一整块内存，但在分配的时候，被我们虚拟成了一个二叉树，叶子结点就是2048个8KB大小的Page，分配也就是
    //分配的这些叶子结点，但是这个二叉树的深度会帮助我们快速分配内存，以及计算分配的内存在这块Chunk中的偏移量
    //是一个相当重要的属性值
    private final int maxOrder;

    //每一个叶子结点的大小，8KB
    final int pageSize;

    //这个值为13，是用来辅助计算所分配的内存大小在二叉树的第几层，也是个非常重要的属性
    final int pageShifts;

    //Chunk的大小，为16MB
    final int chunkSize;

    //这个值是个掩码，辅助计算用户申请的内存是大于8Kb的，还是小于8KB的。在具体方法内给大家标记了详细的注释
    //大家可以在方法中去学习该值的用法，在构造函数中被赋值，为-8192
    final int subpageOverflowMask;

    //这个值就是smallSubpagePools数组的容量，值为4，因为smallSubpagePools数组只存放
    //512B，1KB，2KB，4KB的内存大小
    final int numSmallSubpagePools;

    // 直接内存对齐
    final int directMemoryCacheAlignment;
    final int directMemoryCacheAlignmentMask;

    //存放Tiny内存大小的PoolSubpage数组，容量为32，并且数组中的每个位置存储的都是一个PoolSubpage的双向链表
    //只有相同规格的PoolSubpage才会存储在同一个双向链表内
    private final PoolSubpage<T>[] tinySubpagePools;

    //存放Small内存大小的PoolSubpage数组，容量为4，并且数组中的每个位置存储的都是一个PoolSubpage的双向链表
    //只有相同规格的PoolSubpage才会存储在同一个双向链表内
    private final PoolSubpage<T>[] smallSubpagePools;

    //内存使用率为50%到100%的Chunk集合
    private final PoolChunkList<T> q050;
    //内存使用率为25%到75%的Chunk集合
    private final PoolChunkList<T> q025;
    //内存使用率为1%到50%%的Chunk集合
    private final PoolChunkList<T> q000;
    //内存使用率为0到25%的Chunk集合
    private final PoolChunkList<T> qInit;
    //内存使用率为75%到100%的Chunk集合
    private final PoolChunkList<T> q075;
    //内存使用率为100%的Chunk集合
    private final PoolChunkList<T> q100;

    private final List<PoolChunkListMetric> chunkListMetrics;

    private long allocationsNormal;
    //以下属性都是用来计数的
    private final LongCounter allocationsTiny = PlatformDependent.newLongCounter();
    private final LongCounter allocationsSmall = PlatformDependent.newLongCounter();
    private final LongCounter allocationsHuge = PlatformDependent.newLongCounter();
    private final LongCounter activeBytesHuge = PlatformDependent.newLongCounter();

    private long deallocationsTiny;
    private long deallocationsSmall;
    private long deallocationsNormal;

    private final LongCounter deallocationsHuge = PlatformDependent.newLongCounter();

    //这个值很重要，还记得我们在给线程创建它自己的内存池时，会寻找被其他线程引用最少的PoolArena来使用吗
    //这个原子类记录的就是该PoolArena正被多少单线程执行器引用
    final AtomicInteger numThreadCaches = new AtomicInteger();


    /**
     * :PoolArena类的构造函数，这个构造函数是不会直接调用的，我们会在PooledByteBufAllocator类中 创建PoolArena.HeapArena对象或者是PoolArena.DirectArena对象，会调用HeapArena或DirectArena的构造函数
     * 进而调用到这两个类的父类的PoolArena构造函数中
     *
     * @param parent
     * @param pageSize
     * @param maxOrder
     * @param pageShifts
     * @param chunkSize
     * @param cacheAlignment
     */
    protected PoolArena(PooledByteBufAllocator parent, int pageSize, int maxOrder, int pageShifts, int chunkSize, int cacheAlignment) {
        //parent赋值到这里，表明创建的这个PoolArena是属于哪个内存分配器
        this.parent = parent;
        //下面这些都是一些简单的赋值操作，具体逻辑可以从该类的子类的构造函数中查看。下面的大部分属性我们都已经讲解过了
        this.pageSize = pageSize;
        this.maxOrder = maxOrder;
        this.pageShifts = pageShifts;
        this.chunkSize = chunkSize;

        directMemoryCacheAlignment = cacheAlignment;
        directMemoryCacheAlignmentMask = cacheAlignment - 1;

        // 辅助计算用户申请的内存是大于8Kb的，还是小于8KB的。
        subpageOverflowMask = ~(pageSize - 1);

        //创建tinySubpagePools数组，数组容量为32
        tinySubpagePools = newSubpagePoolArray(numTinySubpagePools);
        for (int i = 0; i < tinySubpagePools.length; i++) {
            //之前我们说了，数组的每一位存储的都是一个SubpagePool的链表
            //所以我们要给数组的每一位先创建好链表的头节点，但是头节点不参与内存分配
            tinySubpagePools[i] = newSubpagePoolHead(pageSize);
        }

        //得到smallSubpagePools数组的容量
        numSmallSubpagePools = pageShifts - 9;
        //创建smallSubpagePools数组，数组容量为4
        smallSubpagePools = newSubpagePoolArray(numSmallSubpagePools);
        //下面的逻辑同tinySubpagePools数组那里相同
        for (int i = 0; i < smallSubpagePools.length; i++) {
            smallSubpagePools[i] = newSubpagePoolHead(pageSize);
        }


        //这里初始化了六个PoolChunkList，而PoolChunkList中存储的就是PoolChunk
        //这里有一个很有意思的地方，就是每一个PoolChunkList的PoolChunk的使用率都有一定的重叠
        //之所以这样设计，还是为了避免PoolChunk在这几个链表中频繁的移动位置。因为当一个PoolChunk的内存利用率
        //改变了，就会视情况移动到符合其内存使用率的PoolChunkList中，如果这六个List的内存使用率没有重叠，
        //如果一个PoolChunk的内存使用率在两个紧邻的范围间来回波动，是不是就要频繁地在两个List中移动这个PoolChunk？
        //这么做显然不太合适，所以，采用重叠的内存使用率，可以避免这种情况出现
        q100 = new PoolChunkList<T>(this, null, 100, Integer.MAX_VALUE, chunkSize);
        q075 = new PoolChunkList<T>(this, q100, 75, 100, chunkSize);
        q050 = new PoolChunkList<T>(this, q075, 50, 100, chunkSize);
        q025 = new PoolChunkList<T>(this, q050, 25, 75, chunkSize);
        q000 = new PoolChunkList<T>(this, q025, 1, 50, chunkSize);
        qInit = new PoolChunkList<T>(this, q000, Integer.MIN_VALUE, 25, chunkSize);

        //这里可以看到，虽然PoolChunkList是集合，但是这六个集合实际上组成了一个节点为PoolChunkList的链表
        //头节点为qInit，下面依次为q000，q025，q050，q075，q100
        q100.prevList(q075);
        q075.prevList(q050);
        q050.prevList(q025);
        q025.prevList(q000);
        //该PoolChunkList没有前置节点，这意味着当q000中的PoolChunk的内存使用率过低，整个PoolChunk就会被释放了
        //不会再存在于链表中
        q000.prevList(null);
        //这里可以看到qInit的前置节点是自己，这意味着当qInit中的PoolChunk的内存使用率低于临界值，仍然会在自身中，不会被释放
        //只有进入了q000集合中的Chunk才会被释放，这个和上面那个q000配合使用，可以做到一直保留一个Chunk在内存中，又可以及时释放
        //大块的Chunk内存共别处使用，属于是两头都吃了，很鸡贼啊
        qInit.prevList(qInit);

        List<PoolChunkListMetric> metrics = new ArrayList<>(6);
        metrics.add(qInit);
        metrics.add(q000);
        metrics.add(q025);
        metrics.add(q050);
        metrics.add(q075);
        metrics.add(q100);
        chunkListMetrics = Collections.unmodifiableList(metrics);
    }


    /**
     * 创建PoolSubpage链表的头节点
     *
     * @param pageSize
     * @return
     */
    private PoolSubpage<T> newSubpagePoolHead(int pageSize) {
        PoolSubpage<T> head = new PoolSubpage<T>(pageSize);
        head.prev = head;
        head.next = head;
        return head;
    }

    @SuppressWarnings("unchecked")
    private PoolSubpage<T>[] newSubpagePoolArray(int size) {
        return new PoolSubpage[size];
    }

    /**
     * 是否直接内存
     *
     * @return
     */
    abstract boolean isDirect();

    protected abstract PooledByteBuf<T> newByteBuf(int maxCapacity);


    protected abstract void memoryCopy(T src, int srcOffset, T dst, int dstOffset, int length);


    protected abstract PoolChunk<T> newUnpooledChunk(int capacity);


    protected abstract PoolChunk<T> newChunk(int pageSize, int maxOrder, int pageShifts, int chunkSize);


    protected abstract void destroyChunk(PoolChunk<T> chunk);

    /**
     * 分配内存的方法
     *
     * @param cache
     * @param reqCapacity
     * @param maxCapacity
     * @return
     */
    PooledByteBuf<T> allocate(PoolThreadCache cache, int reqCapacity, int maxCapacity) {
        //得到一个池化的ByteBuf，这里就会和我们之前学的对象池连接起来了，因为newByteBuf该方法会
        //调用到PooledDirectByteBuf的newInstance方法内，在该方法内就是从线程私有的对象池内获得一个ByteBuf对象
        PooledByteBuf<T> buf = newByteBuf(maxCapacity);
        //给ByteBuf分配内存，这里大家可以看到，我们分配的是直接内存，而该内存是被一个ByteBuf包装着的
        //并不是创建了一个ByteBuf，让ByteBuf申请内存，这一点到这里大家可能还不太理解，但往下看，看到内存时怎么被ByteBuf包装的时候
        //大家就会清楚了
        allocate(cache, buf, reqCapacity);
        return buf;
    }

    /**
     * 具体分配内存的方法，这里其实仍然是一些流程步骤，我们之前说了，真正分配内存的方法，是在PoolChunk中进行的
     *
     * @param cache
     * @param buf
     * @param reqCapacity
     */
    private void allocate(PoolThreadCache cache, PooledByteBuf<T> buf, final int reqCapacity) {
        //netty中用到了伙伴算法来分配内存，这个方法就起到了规整要申请的内存的作用
        //得到规整之后的内存，所以，这里大家也可以清楚了，并不是用户申请的多少内存就分配多少
        //分配系统会自动补全要分配的内存
        final int normCapacity = normalizeCapacity(reqCapacity);

        //判断要分配的内存是多大，是不是tiny或者small大小的
        if (isTinyOrSmall(normCapacity)) {
            //走到这里说明是tiny或者small大小的，要继续细分

            int tableIdx;
            PoolSubpage<T>[] table;
            //判断要申请的内存是否为tiny范围大小的
            boolean tiny = isTiny(normCapacity);

            if (tiny) {
                //走到这里说明是tiny大小的
                //先从内存池中分配，分配成功就直接返回，不成功就继续向下执行，内存池分配内存的逻辑后面具体再看
                if (cache.allocateTiny(this, buf, reqCapacity, normCapacity)) {
                    //大家可以看到，这里实际上是没有返回值的，还记得我们之前从线程私有的对象池中获得的ByteBuf吗？
                    //上面的方法中了，这进一步说明用户申请的内存实际上是被包装到ByteBuf中了，并不是说ByteBuf就是一块内存
                    return;
                }
                //内存池分配不成功就会走到这里
                //这里把要分配的内存传进去，得到一个数组的下标，实际上就是tinySubpagePools数组的下标
                //这个下标，意味着要分配的内存在tinySubpagePools数组的哪个位置
                //因为tinySubpagePools数组内的下标对应的是[0，1，2....31]，以16递增的
                tableIdx = tinyIdx(normCapacity);
                //把tinySubpagePools数组赋值给上面定义的那个table
                table = tinySubpagePools;
            } else {
                //走到这里说明要分配的内存大小在small范围内
                if (cache.allocateSmall(this, buf, reqCapacity, normCapacity)) {
                    //同样是在内存池中分配成功后就可以直接返回了
                    return;
                }
                //内存池分配失败，则计算要分配的内存在smallSubpagePools数组中的下标位置
                tableIdx = smallIdx(normCapacity);
                //把smallSubpagePools数组赋值给上面定义的那个table
                table = smallSubpagePools;
            }

            //这里得到了双向链表的头节点，索引在上面确定了，无论是从smallSubpagePools数组还是从tinySubpagePools数组分配内存
            //都要先得到数组下标的头节点，头节点不参与分配的
            final PoolSubpage<T> head = table[tableIdx];

            //加锁，并且是以头节点为锁的。注意，我们说内存池也是每个线程私有的，但这里并不是从内存池中分配，是多个线程
            //从这个PoolArena中申请内存，所以就要考虑并发问题，自然要加锁
            synchronized (head) {

                //得到头节点的下一个节点
                final PoolSubpage<T> s = head.next;

                //我们之前说过，头节点不参与内存分配，所以如果下一个节点就是头节点，说明该链表没有可以用来分配的PoolSubpage节点
                //因为PoolSubpage中的内存分配完了，该节点就会从链表中删除
                if (s != head) {
                    //这里是判断一下PoolSubpage对象没被销毁，并且PoolSubpage是以normCapacity内存大小分割的
                    //这里大家可能还不太懂什么意思，我们到了PoolSubpage类中会继续讲解，到时候大家就会发现串起来了
                    assert s.doNotDestroy && s.elemSize == normCapacity;

                    //从PoolSubpage节点中分配内存，具体的方法在PoolSubpage类中
                    //得到一个内存偏移量
                    long handle = s.allocate();
                    assert handle >= 0;

                    //初始化ByteBuf，实际上就是把申请的直接内存交给ByteBuf包装，具体逻辑我们后面再看
                    s.chunk.initBufWithSubpage(buf, null, handle, reqCapacity);
                    //分配的tiny内存次数加1或者是small内存的次数加1
                    incTinySmallAllocation(tiny);
                    return;
                }
            }
            //走到这里说明上面从PoolSubpage节点中分配内存失败了，可能是根本就还没有PoolSubpage节点可供内存分配呢
            //所以，这里要从Chunk中分配内存。
            //这里大家是不是也可以把之前的逻辑串起来了？判断内存分配的大小，如果太小，就先从tiny或者small数组中分配小内存
            //如果数组分配内存失败，才会从Chunk中分配。但是大家还要记住，从Chunk中分配出来的，实际上还是一个PoolSubpage对象，这个队形
            //会添加到PoolSubpage数组中相应的链表中，这个逻辑到后面也会串起来，这里大家先记住就行
            synchronized (this) {
                //上面分配内存失败，所以这里就要直接从Chunk中申请一个8KB的内存空间
                //这块内存实际上会交给PoolSubpage包装，而PoolSubpage会添加到数组的链表中
                allocateNormal(buf, reqCapacity, normCapacity);
            }
            incTinySmallAllocation(tiny);
            return;
        }

        if (normCapacity <= chunkSize) {
            //走到这里说明分配的内存是大于8KB小于16MB的
            //先从内存池中分配内存
            if (cache.allocateNormal(this, buf, reqCapacity, normCapacity)) {
                //分配成功直接返回
                return;
            }
            //走到这里说明没有从内存池中分配成功，就会从ChunkList中尝试分配内存
            synchronized (this) {
                allocateNormal(buf, reqCapacity, normCapacity);
                ++allocationsNormal;
            }
        } else {
            //走到这里则说明要分配的内存是大于16MB的，这时候就要直接申请一个Huge块的内存，这个内存用完就被释放了
            allocateHuge(buf, reqCapacity);
        }
    }


    /**
     * 释放内存的方法，注意，这里释放内存并不是释放的Chunk内存，而是释放的normCapacity内存，千万不要搞混了 这里判断Chunk内存块是否可以被池化，
     * 并不是说要把Chunk放到内存池中，而是把从Chunk中申请的内存放到内存池中
     *
     * @param chunk
     * @param nioBuffer
     * @param handle
     * @param normCapacity
     * @param cache
     */
    void free(PoolChunk<T> chunk, ByteBuffer nioBuffer, long handle, int normCapacity, PoolThreadCache cache) {
        //判断Chunk内存快是否为可以池化的
        if (chunk.unpooled) {
            //得到Chunk块的大小
            int size = chunk.chunkSize();
            //如果不是池化的，直接销毁即可
            destroyChunk(chunk);
            //已经分配的内存减去销毁的内存大小
            activeBytesHuge.add(-size);
            //回收了几次大块内存自增
            deallocationsHuge.increment();
        } else {
            //走到这里说明是可以池化的Chunk内存快
            //计算要被释放的内存的大小在哪个范围内
            SizeClass sizeClass = sizeClass(normCapacity);
            //放到内存池中
            if (cache != null && cache.add(this, chunk, nioBuffer, handle, normCapacity, sizeClass)) {
                //成功则返回
                return;
            }
            //如果当前线程的内存池无法再缓存内存，就把要释放的内存还给Chunk内存块。它从哪个Chunk内存块分配的就归还给谁
            freeChunk(chunk, handle, sizeClass, nioBuffer, false);
        }
    }

    /**
     * 计算内存所属的内存范围的类型，是tiny还是small还是normal
     *
     * @param chunk
     * @param handle
     * @param sizeClass
     * @param nioBuffer
     * @param finalizer
     */
    void freeChunk(PoolChunk<T> chunk, long handle, SizeClass sizeClass, ByteBuffer nioBuffer, boolean finalizer) {
        final boolean destroyChunk;
        //这里加了一个同步锁，是因为该方法会进一步调用到
        //!chunk.parent.free(chunk, handle, nioBuffer)这行代码，而在free方法内，会涉及到Chunk内存块在
        //链表间的移动，所以加一个同步锁，防止出现多个线程释放内存，Chunk块的范围改动出现并发问题，导致移动也出现问题
        synchronized (this) {
            if (!finalizer) {
                switch (sizeClass) {
                    //下面这几个自增都是引用计数增加，回收normal和tiny和samll的次数自增
                    case Normal:
                        ++deallocationsNormal;
                        break;
                    case Small:
                        ++deallocationsSmall;
                        break;
                    case Tiny:
                        ++deallocationsTiny;
                        break;
                    default:
                        throw new Error();
                }
            }
            //这里其实是调用了PoolChunkList的free方法
            //chunk.parent返回的就是Chunk所属的PoolChunkList链表对象
            destroyChunk = !chunk.parent.free(chunk, handle, nioBuffer);
        }
        if (destroyChunk) {
            //销毁内存则不需要使用同步锁
            destroyChunk(chunk);
        }
    }

    /**
     * 计算内存所属的内存范围的类型，是tiny还是small还是normal
     *
     * @param normCapacity
     * @return
     */
    private SizeClass sizeClass(int normCapacity) {
        if (!isTinyOrSmall(normCapacity)) {
            return SizeClass.Normal;
        }
        return isTiny(normCapacity) ? SizeClass.Tiny : SizeClass.Small;
    }


    /**
     * 从Chunk中申请内存的方法
     *
     * @param buf
     * @param reqCapacity
     * @param normCapacity
     */
    private void allocateNormal(PooledByteBuf<T> buf, int reqCapacity, int normCapacity) {
        //这里的优先使用哪个利用率范围的Chunk来分配内存也是很有讲究的，这里可以明显看到，是先从q050这个Chunk链表中的Chunk
        //中分配的，之前我们提到过，Netty的内存分配有其自身的均衡之道，优先使用利用率范围在中间的Chunk，然后使用q025和q000以提高内存
        //利用率，之所以把qInit放在第四是因为该Chunk链表中的Chunk不会被回收。而q075放在最后是因为这个链表中的Chunk使用率比较高
        //分配内配不容易成功。大家可以在仔细品味品味。
        if (
                q050.allocate(buf, reqCapacity, normCapacity) ||
                q025.allocate(buf, reqCapacity, normCapacity) ||
                q000.allocate(buf, reqCapacity, normCapacity) ||
                qInit.allocate(buf, reqCapacity, normCapacity) ||
                q075.allocate(buf, reqCapacity, normCapacity)
        ) {
            return;
        }
        //第一次分配内存的时候，Chunk链表应该还没有Chunk，所以不会分配成功，自然会走到这里，创建一个Chunk
        PoolChunk<T> c = newChunk(pageSize, maxOrder, pageShifts, chunkSize);
        //在这里，从新创建的Chunk中分配一块用户申请的内存。
        //这里请大家思考一下，这个地方的内存分配有没有可能分配tiny和small大小的内存呢？
        //当然是有可能的，这就要涉及到后面的一个知识点了。PoolSubpage，不管是tiny还是small类型的，都是从
        //Chunk中分配出来的，所以说Chunk是内存分配的起点，如果没有Chunk，那就不会有PoolSubpage。那PoolSubpage数组中
        //就不会有可以分配内存的PoolSubpage。当第一次分配的内存的时候，还没有Chunk，PoolSubpage自然也不会有，所以会创建一个
        //新的Chunk，从Chunk中分配一个PoolSubpage，再从PoolSubpage分配tiny或者samll大小的内存。这个知识点要记住
        //当然我们这里分析的知识分配tiny和samll大小的内存
        boolean success = c.allocate(buf, reqCapacity, normCapacity);
        assert success;
        //注意，这里把创建的Chunk内存块首先加入到了qInit这个链表中，如果这个链表中的Chunk块的内存使用率一直在qInit范围内，
        //那它就不会被移动，会一直呆在qInit链表中，也就不会被释放回收
        qInit.add(c);
    }


    /**
     * 分配超过16Mb的huge大小的内存
     *
     * @param buf
     * @param reqCapacity
     */
    private void allocateHuge(PooledByteBuf<T> buf, int reqCapacity) {
        PoolChunk<T> chunk = newUnpooledChunk(reqCapacity);
        activeBytesHuge.add(chunk.chunkSize());
        buf.initUnpooled(chunk, reqCapacity);
        allocationsHuge.increment();
    }


    /**
     * 计数器+1
     *
     * @param tiny
     */
    private void incTinySmallAllocation(boolean tiny) {
        if (tiny) {
            allocationsTiny.increment();
        } else {
            allocationsSmall.increment();
        }
    }

    /**
     * 计算要分配的内存在tinySubpagePools数组中的下标位置 右移4位就是除以16。该内存经过规整，可以被16整除
     *
     * @param normCapacity
     * @return
     */
    static int tinyIdx(int normCapacity) {
        return normCapacity >>> 4;
    }


    /**
     * 计算要分配的内存在smallSubpagePools数组的下标位置 这个就是具体的位运算逻辑了，举例子运算即可
     *
     * @param normCapacity
     * @return
     */
    static int smallIdx(int normCapacity) {
        //small范围的内存为512-4KB，，所以我们以512为例来计算下标
        //这个范围的值是以乘以2递增的，所以512确定了低十位为1，那后面的几个数分别就为低11位为1，低12位为1，低13位为1
        //右移十位，1所在的位置正好对应数组下标的位置
        int tableIdx = 0;
        //512的二进制 0000 0000 0000 0000 0000 0010 0000 0000
        //右移十位 0000 0000 0000 0000 0000 0000 0000 0000
        //1024的二进制 0000 0000 0000 0000 0000 0100 0000 0000
        //右移十位 0000 0000  0000 0000 0000 0000 0000 0001
        //下面的逻辑很简单，就不再写了
        int i = normCapacity >>> 10;
        while (i != 0) {
            i >>>= 1;
            tableIdx++;
        }
        return tableIdx;
    }

    /**
     * 判断要申请的内存是否小于8KB
     *
     * @param normCapacity
     * @return
     */
    boolean isTinyOrSmall(int normCapacity) {
        //subpageOverflowMask这个属性的值为-8192，这个我们之前已经说过了
        //如果是-8192，那换成二进制就是1111 1111 1111 1111 1110 0000 0000 0000
        //注意，这里做的是与运算，所以就会拿要申请的内存值跟-8192做与运算
        //如果要申请的内存的大小是8kB，换算成二进制就是0000 0000 0000 0000 0010 0000 0000 0000
        //做与运算得到的结果就是0000 0000 0000 0000 0010 0000 0000 0000
        //可以看到该值是不等于0的，但如果要申请的内存小于8KB，这样的话，该值二进制的低14位一定不为一，剩下的低13位即便有1也无济于事
        //得到的仍然为0
        //所以，这里就用做与运算的值是否为0判断要申请的内存是否小于8KB
        //如果返回true则说明要申请的是tin或者small类型的内存，然后再继续细分是tiny还是small
        return (normCapacity & subpageOverflowMask) == 0;
    }


    /**
     * 规整申请内存的方法,也会是内存对齐
     *
     * @param reqCapacity
     * @return
     */
    int normalizeCapacity(int reqCapacity) {
        //判断要申请的内存必须是大于0的
        checkPositiveOrZero(reqCapacity, "reqCapacity");
        //这个是针对大于16MB的内存的的规整，我们暂时还不需要关注这里
        //其实这里就是如果用户申请的是大于16MB，就会直接申请这个内存
        if (reqCapacity >= chunkSize) {
            return directMemoryCacheAlignment == 0 ? reqCapacity : alignCapacity(reqCapacity);
        }
        //判断申请的内存是否为Tiny大小的
        if (!isTiny(reqCapacity)) {
            //这里进入分支，说明要申请的内存不是tiny类型的，得到要申请的内存
            int normalizedCapacity = reqCapacity;
            //这里做了个--的操作，目的是如果申请内的内存本身就是2的n次幂，这么做就会省去一些时间？原因待定，来个算法好的解释一下
            //我们举两个例子来看一下，第一个例子，以1024为申请的内存大小
            //二进制         0000 0000 0000 0000 0000 0100 0000 0000
            //做了--操作后    0000 0000 0000 0000 0000 0011 1111 1111
            //之后不管1023怎么右移，和1023做位运算，得到的总是本身，最后在做个++操作，就得到1024了
            //如果是别的值，大家可以自己测一测，我弄了一个测试类Test，专门测试这段代码
            //这里之所以右移32为，是因为不知道用户申请的那个内存中高位中的第一个1在哪个位置，索性右移32位
            //只要遇到那个高位1了，再怎么右移也不会对结果产生变化 这里逻辑有点绕，需要多看几遍
            normalizedCapacity--;
            normalizedCapacity |= normalizedCapacity >>> 1;
            normalizedCapacity |= normalizedCapacity >>> 2;
            normalizedCapacity |= normalizedCapacity >>> 4;
            normalizedCapacity |= normalizedCapacity >>> 8;
            normalizedCapacity |= normalizedCapacity >>> 16;
            normalizedCapacity++;
            //判断normalizedCapacity是否溢出了，溢出就右移
            if (normalizedCapacity < 0) {
                normalizedCapacity >>>= 1;
            }
            assert directMemoryCacheAlignment == 0 || (normalizedCapacity & directMemoryCacheAlignmentMask) == 0;
            return normalizedCapacity;
        }
        if (directMemoryCacheAlignment > 0) {
            return alignCapacity(reqCapacity);
        }
        //这里的逻辑就简单一些了，走到这里就意味着要申请的内存是tiny大小的
        //tiny是按16B递增的，所以判断能够被16整除，可以整除就能直接返回
        if ((reqCapacity & 15) == 0) {
            return reqCapacity;
        }
        //这里也涉及到一个位运算，首先我们要弄清楚15取反的值
        //～15的二进制  1111 1111 1111 1111 1111 1111 1111 0000
        //如果reqCapacity小于16，那这个数的二进制的低五位肯定不是1，所以做与运算结果肯定为0
        //所以，这里的意思就是如果分配的内存小于16B，那就直接返回16B
        //而如果申请的内存是大于十六的，做了与运算后得到的就是个16的倍数，再加上16仍然是16的倍数，直接返回即可
        return (reqCapacity & ~15) + 16;
    }

    /**
     * 判断要申请的内存是否小于512B
     *
     * @param normCapacity
     * @return
     */
    static boolean isTiny(int normCapacity) {
        //0xFFFFFE00是个十六进制的数，换算成二进制为1111 1111 1111 1111 1111 1110 0000 0000
        //512的二进制为                          0000 0000 0000 0000 0000 0101 0001 0010
        //496的二进制为                          0000 0000 0000 0000 0000 0001 1111 0000
        //原理与上面相同，就不再赘述了
        return (normCapacity & 0xFFFFFE00) == 0;
    }


    /**
     * @param reqCapacity
     * @return
     */
    int alignCapacity(int reqCapacity) {
        int delta = reqCapacity & directMemoryCacheAlignmentMask;
        return delta == 0 ? reqCapacity : reqCapacity + directMemoryCacheAlignment - delta;
    }


    /**
     * 这是一个用于重新分配内存的方法。它接受一个PooledByteBuf对象、一个新的容量值和一个布尔值参数来指示是否释放旧的内存。
     * 首先，它会检查新的容量值是否合法，如果不合法则抛出一个IllegalArgumentException异常。
     * 接下来，它会获取旧的容量值，并与新的容量值进行比较。如果它们相等，则直接返回。
     * 然后，它会保存旧的Chunk、旧的NIO缓冲区、旧的句柄、旧的内存、旧的偏移量、旧的最大长度、读索引和写索引的值。
     * 接着，它会调用allocate方法来分配新的内存，并根据新旧容量的大小关系进行内存拷贝操作。如果新容量大于旧容量，它会将旧内存中的数据拷贝到新内存中。如果新容量小于旧容量，它会根据读索引和写索引的位置进行部分数据的拷贝。
     * 然后，它会更新PooledByteBuf对象的读写索引。
     * 最后，如果freeOldMemory参数为true，它会释放旧的内存。
     *
     * @param buf
     * @param newCapacity
     * @param freeOldMemory
     */
    void reallocate(PooledByteBuf<T> buf, int newCapacity, boolean freeOldMemory) {
        if (newCapacity < 0 || newCapacity > buf.maxCapacity()) {
            throw new IllegalArgumentException("newCapacity: " + newCapacity);
        }

        int oldCapacity = buf.length;
        if (oldCapacity == newCapacity) {
            return;
        }

        PoolChunk<T> oldChunk = buf.chunk;
        ByteBuffer oldNioBuffer = buf.tmpNioBuf;
        long oldHandle = buf.handle;
        T oldMemory = buf.memory;
        int oldOffset = buf.offset;
        int oldMaxLength = buf.maxLength;
        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();

        allocate(parent.threadCache(), buf, newCapacity);
        if (newCapacity > oldCapacity) {
            memoryCopy(oldMemory, oldOffset, buf.memory, buf.offset, oldCapacity);
        } else if (newCapacity < oldCapacity) {
            if (readerIndex < newCapacity) {
                if (writerIndex > newCapacity) {
                    writerIndex = newCapacity;
                }
                memoryCopy(oldMemory, oldOffset + readerIndex, buf.memory, buf.offset + readerIndex, writerIndex - readerIndex);
            } else {
                readerIndex = writerIndex = newCapacity;
            }
        }

        buf.setIndex(readerIndex, writerIndex);

        if (freeOldMemory) {
            free(oldChunk, oldNioBuffer, oldHandle, oldMaxLength, buf.cache);
        }
    }

    @Override
    public synchronized String toString() {
        StringBuilder buf = new StringBuilder().append("Chunk(s) at 0~25%:").append(StringUtil.NEWLINE).append(qInit).append(StringUtil.NEWLINE).append("Chunk(s) at 0~50%:").append(StringUtil.NEWLINE).append(q000).append(StringUtil.NEWLINE).append("Chunk(s) at 25~75%:").append(StringUtil.NEWLINE).append(q025).append(StringUtil.NEWLINE).append("Chunk(s) at 50~100%:").append(StringUtil.NEWLINE).append(q050).append(StringUtil.NEWLINE).append("Chunk(s) at 75~100%:").append(StringUtil.NEWLINE).append(q075).append(StringUtil.NEWLINE).append("Chunk(s) at 100%:").append(StringUtil.NEWLINE).append(q100).append(StringUtil.NEWLINE).append("tiny subpages:");
        appendPoolSubPages(buf, tinySubpagePools);
        buf.append(StringUtil.NEWLINE).append("small subpages:");
        appendPoolSubPages(buf, smallSubpagePools);
        buf.append(StringUtil.NEWLINE);

        return buf.toString();
    }

    private static void appendPoolSubPages(StringBuilder buf, PoolSubpage<?>[] subpages) {
        for (int i = 0; i < subpages.length; i++) {
            PoolSubpage<?> head = subpages[i];
            if (head.next == head) {
                continue;
            }

            buf.append(StringUtil.NEWLINE).append(i).append(": ");
            PoolSubpage<?> s = head.next;
            for (; ; ) {
                buf.append(s);
                s = s.next;
                if (s == head) {
                    break;
                }
            }
        }
    }


    @Override
    protected final void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            destroyPoolSubPages(smallSubpagePools);
            destroyPoolSubPages(tinySubpagePools);
            destroyPoolChunkLists(qInit, q000, q025, q050, q075, q100);
        }
    }

    private static void destroyPoolSubPages(PoolSubpage<?>[] pages) {
        for (PoolSubpage<?> page : pages) {
            page.destroy();
        }
    }

    private void destroyPoolChunkLists(PoolChunkList<T>... chunkLists) {
        for (PoolChunkList<T> chunkList : chunkLists) {
            chunkList.destroy(this);
        }
    }


    //    ----------------------接口实现
    @Override
    public int numThreadCaches() {
        return numThreadCaches.get();
    }

    @Override
    public int numTinySubpages() {
        return tinySubpagePools.length;
    }

    @Override
    public int numSmallSubpages() {
        return smallSubpagePools.length;
    }

    @Override
    public int numChunkLists() {
        return chunkListMetrics.size();
    }

    @Override
    public List<PoolSubpageMetric> tinySubpages() {
        return subPageMetricList(tinySubpagePools);
    }

    @Override
    public List<PoolSubpageMetric> smallSubpages() {
        return subPageMetricList(smallSubpagePools);
    }

    @Override
    public List<PoolChunkListMetric> chunkLists() {
        return chunkListMetrics;
    }

    private static List<PoolSubpageMetric> subPageMetricList(PoolSubpage<?>[] pages) {
        List<PoolSubpageMetric> metrics = new ArrayList<PoolSubpageMetric>();
        for (PoolSubpage<?> head : pages) {
            if (head.next == head) {
                continue;
            }
            PoolSubpage<?> s = head.next;
            for (; ; ) {
                metrics.add(s);
                s = s.next;
                if (s == head) {
                    break;
                }
            }
        }
        return metrics;
    }

    @Override
    public long numAllocations() {
        final long allocsNormal;
        synchronized (this) {
            allocsNormal = allocationsNormal;
        }
        return allocationsTiny.value() + allocationsSmall.value() + allocsNormal + allocationsHuge.value();
    }

    @Override
    public long numTinyAllocations() {
        return allocationsTiny.value();
    }

    @Override
    public long numSmallAllocations() {
        return allocationsSmall.value();
    }

    @Override
    public synchronized long numNormalAllocations() {
        return allocationsNormal;
    }

    @Override
    public long numDeallocations() {
        final long deallocs;
        synchronized (this) {
            deallocs = deallocationsTiny + deallocationsSmall + deallocationsNormal;
        }
        return deallocs + deallocationsHuge.value();
    }

    @Override
    public synchronized long numTinyDeallocations() {
        return deallocationsTiny;
    }

    @Override
    public synchronized long numSmallDeallocations() {
        return deallocationsSmall;
    }

    @Override
    public synchronized long numNormalDeallocations() {
        return deallocationsNormal;
    }

    @Override
    public long numHugeAllocations() {
        return allocationsHuge.value();
    }

    @Override
    public long numHugeDeallocations() {
        return deallocationsHuge.value();
    }

    @Override
    public long numActiveAllocations() {
        long val = allocationsTiny.value() + allocationsSmall.value() + allocationsHuge.value() - deallocationsHuge.value();
        synchronized (this) {
            val += allocationsNormal - (deallocationsTiny + deallocationsSmall + deallocationsNormal);
        }
        return max(val, 0);
    }

    @Override
    public long numActiveTinyAllocations() {
        return max(numTinyAllocations() - numTinyDeallocations(), 0);
    }

    @Override
    public long numActiveSmallAllocations() {
        return max(numSmallAllocations() - numSmallDeallocations(), 0);
    }

    @Override
    public long numActiveNormalAllocations() {
        final long val;
        synchronized (this) {
            val = allocationsNormal - deallocationsNormal;
        }
        return max(val, 0);
    }

    @Override
    public long numActiveHugeAllocations() {
        return max(numHugeAllocations() - numHugeDeallocations(), 0);
    }

    @Override
    public long numActiveBytes() {
        long val = activeBytesHuge.value();
        synchronized (this) {
            for (int i = 0; i < chunkListMetrics.size(); i++) {
                for (PoolChunkMetric m : chunkListMetrics.get(i)) {
                    val += m.chunkSize();
                }
            }
        }
        return max(0, val);
    }


    /**
     * 从PoolArena持有的PoolSubpage数组中寻找合适的分配链表 这里返回的就是可以提供可分配内存的PoolSubpage数组中对应位置的链表头节点
     * 这个逻辑也很简单，elemSize参数实际上就是用户要分配的内存，已经经过规整了，所以
     * 直接判断PoolSubpage数组中哪个索引对应的PoolSubpage链表是以elemSize平均分配的即可
     * 大家自己看看这个方法就行了
     *
     * @param elemSize
     * @return
     */
    PoolSubpage<T> findSubpagePoolHead(int elemSize) {
        int tableIdx;
        PoolSubpage<T>[] table;

        // 判断要申请的内存是否小于512B
        if (isTiny(elemSize)) {
            // 就是除以2 / 2 / 2 / 2 如果是512 = 32
            tableIdx = elemSize >>> 4;
            table = tinySubpagePools;
        } else {
            tableIdx = 0;
            elemSize >>>= 10;
            while (elemSize != 0) {
                elemSize >>>= 1;
                tableIdx++;
            }
            table = smallSubpagePools;
        }

        return table[tableIdx];
    }


    static final class HeapArena extends PoolArena<byte[]> {

        HeapArena(PooledByteBufAllocator parent, int pageSize, int maxOrder, int pageShifts, int chunkSize, int directMemoryCacheAlignment) {
            super(parent, pageSize, maxOrder, pageShifts, chunkSize, directMemoryCacheAlignment);
        }

        private static byte[] newByteArray(int size) {
            return PlatformDependent.allocateUninitializedArray(size);
        }

        @Override
        boolean isDirect() {
            return false;
        }

        @Override
        protected PoolChunk<byte[]> newChunk(int pageSize, int maxOrder, int pageShifts, int chunkSize) {
            return new PoolChunk<>(this, newByteArray(chunkSize), pageSize, maxOrder, pageShifts, chunkSize, 0);
        }

        @Override
        protected PoolChunk<byte[]> newUnpooledChunk(int capacity) {
            return new PoolChunk<>(this, newByteArray(capacity), capacity, 0);
        }

        @Override
        protected void destroyChunk(PoolChunk<byte[]> chunk) {
            // Rely on GC.
        }

        //这里暂时把这个方法做个简单的改动，因为这节课还不引入PooledUnsafeHeapByteBuf类
        @Override
        protected PooledByteBuf<byte[]> newByteBuf(int maxCapacity) {
            return null;
//                    HAS_UNSAFE ? PooledUnsafeHeapByteBuf.newUnsafeInstance(maxCapacity)
//                    : PooledHeapByteBuf.newInstance(maxCapacity);
        }

        @Override
        protected void memoryCopy(byte[] src, int srcOffset, byte[] dst, int dstOffset, int length) {
            if (length == 0) {
                return;
            }
            System.arraycopy(src, srcOffset, dst, dstOffset, length);
        }
    }


    static final class DirectArena extends PoolArena<ByteBuffer> {

        DirectArena(PooledByteBufAllocator parent, int pageSize, int maxOrder, int pageShifts, int chunkSize, int directMemoryCacheAlignment) {
            super(parent, pageSize, maxOrder, pageShifts, chunkSize, directMemoryCacheAlignment);
        }

        @Override
        boolean isDirect() {
            return true;
        }


        int offsetCacheLine(ByteBuffer memory) {
            int remainder = HAS_UNSAFE ? (int) (PlatformDependent.directBufferAddress(memory) & directMemoryCacheAlignmentMask) : 0;
            return directMemoryCacheAlignment - remainder;
        }

        @Override
        protected PoolChunk<ByteBuffer> newChunk(int pageSize, int maxOrder, int pageShifts, int chunkSize) {
            if (directMemoryCacheAlignment == 0) {
                return new PoolChunk<>(this, allocateDirect(chunkSize), pageSize, maxOrder, pageShifts, chunkSize, 0);
            }

            final ByteBuffer memory = allocateDirect(chunkSize + directMemoryCacheAlignment);
            return new PoolChunk<>(this, memory, pageSize, maxOrder, pageShifts, chunkSize, offsetCacheLine(memory));
        }

        @Override
        protected PoolChunk<ByteBuffer> newUnpooledChunk(int capacity) {
            if (directMemoryCacheAlignment == 0) {
                return new PoolChunk<ByteBuffer>(this, allocateDirect(capacity), capacity, 0);
            }
            final ByteBuffer memory = allocateDirect(capacity + directMemoryCacheAlignment);
            return new PoolChunk<ByteBuffer>(this, memory, capacity, offsetCacheLine(memory));
        }

        /**
         * @Author: ytrue
         * @Description:分配直接内存的方法
         */
        private static ByteBuffer allocateDirect(int capacity) {
            return PlatformDependent.useDirectBufferNoCleaner() ? PlatformDependent.allocateDirectNoCleaner(capacity) : ByteBuffer.allocateDirect(capacity);
        }

        /**
         * @Author: ytrue
         * @Description:销毁Chunk内存块
         */
        @Override
        protected void destroyChunk(PoolChunk<ByteBuffer> chunk) {

            if (PlatformDependent.useDirectBufferNoCleaner()) {
                PlatformDependent.freeDirectNoCleaner(chunk.memory);
            } else {
                PlatformDependent.freeDirectBuffer(chunk.memory);
            }
        }

        // //这里暂时把这个方法做个简单的改动，因为这节课还不引入PooledUnsafeDirectByteBufBuf类
        @Override
        protected PooledByteBuf<ByteBuffer> newByteBuf(int maxCapacity) {
//            if (HAS_UNSAFE) {
//                return PooledUnsafeDirectByteBuf.newInstance(maxCapacity);
//            } else {
//                return PooledDirectByteBuf.newInstance(maxCapacity);
//            }
            return PooledDirectByteBuf.newInstance(maxCapacity);
        }

        @Override
        protected void memoryCopy(ByteBuffer src, int srcOffset, ByteBuffer dst, int dstOffset, int length) {
            if (length == 0) {
                return;
            }
            if (HAS_UNSAFE) {
                PlatformDependent.copyMemory(PlatformDependent.directBufferAddress(src) + srcOffset, PlatformDependent.directBufferAddress(dst) + dstOffset, length);
            } else {
                src = src.duplicate();
                dst = dst.duplicate();
                src.position(srcOffset).limit(srcOffset + length);
                dst.position(dstOffset);
                dst.put(src);
            }
        }
    }
}
