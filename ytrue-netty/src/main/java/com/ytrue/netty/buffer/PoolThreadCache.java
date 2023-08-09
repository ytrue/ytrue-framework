package com.ytrue.netty.buffer;

import com.ytrue.netty.util.Recycler;
import com.ytrue.netty.util.internal.MathUtil;
import com.ytrue.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ytrue.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * @author ytrue
 * @date 2023-08-09 14:16
 * @description 内存池核心类
 */
public class PoolThreadCache {

    private static final Logger logger = LoggerFactory.getLogger(PoolThreadCache.class);
    //持有的使用次数最少的heapArena
    final PoolArena<byte[]> heapArena;

    //持有的使用次数最少的directArena
    final PoolArena<ByteBuffer> directArena;

    //内存池缓存tiny大小内存的容器，对应的是堆内存
    private final MemoryRegionCache<byte[]>[] tinySubPageHeapCaches;
    //内存池缓存small大小内存的容器，对应的是堆内存

    private final MemoryRegionCache<byte[]>[] smallSubPageHeapCaches;
    //内存池缓存tiny大小内存的容器，对应的是直接内存
    private final MemoryRegionCache<ByteBuffer>[] tinySubPageDirectCaches;
    //内存池缓存small大小内存的容器，对应的是直接内存
    private final MemoryRegionCache<ByteBuffer>[] smallSubPageDirectCaches;
    //内存池缓存normal大小内存的容器，对应的是堆内存
    private final MemoryRegionCache<byte[]>[] normalHeapCaches;

    //对应的内存大小为8K,16K,32K，64Kb及以上的内存就不会被缓存了
    //内存池缓存normal大小内存的容器，对应的是直接内存
    private final MemoryRegionCache<ByteBuffer>[] normalDirectCaches;
    //帮助计算normal索引下标的一个辅助性属性，这种属性在PoolChunk中见过一次
    private final int numShiftsNormalDirect;
    private final int numShiftsNormalHeap;
    //当内存池分配出去的内存的次数达到了该阈值，就会清理一次内存池，把没释放的内存都释放了，防止内存无效占用
    //该值为8192，该值是从PooledByteAllocator中传过来的，被DEFAULT_CACHE_TRIM_INTERVAL属性赋值
    private final int freeSweepAllocationThreshold;
    private final AtomicBoolean freed = new AtomicBoolean();
    //记录当前内存池分配出去内存的次数
    private int allocations;

    // TODO: Test if adding padding helps under contention
    //private long pad0, pad1, pad2, pad3, pad4, pad5, pad6, pad7;


    /**
     * @Author: ytrue
     * @Description:构造器方法
     */
    PoolThreadCache(PoolArena<byte[]> heapArena, PoolArena<ByteBuffer> directArena,
                    int tinyCacheSize, int smallCacheSize, int normalCacheSize,
                    int maxCachedBufferCapacity, int freeSweepAllocationThreshold) {
        checkPositiveOrZero(maxCachedBufferCapacity, "maxCachedBufferCapacity");
        //阈值赋值
        this.freeSweepAllocationThreshold = freeSweepAllocationThreshold;
        this.heapArena = heapArena;
        this.directArena = directArena;
        if (directArena != null) {
            //这里说明使用的是直接内存，直接内存可以使用内存池缓存
            //创建tiny类型的缓存数组
            tinySubPageDirectCaches = createSubPageCaches(
                    tinyCacheSize, PoolArena.numTinySubpagePools, PoolArena.SizeClass.Tiny);
            //创建small类型的缓存数组
            smallSubPageDirectCaches = createSubPageCaches(
                    smallCacheSize, directArena.numSmallSubpagePools, PoolArena.SizeClass.Small);
            //这里得到的值为13
            numShiftsNormalDirect = log2(directArena.pageSize);
            //创建normal类型的缓存数组
            normalDirectCaches = createNormalCaches(
                    normalCacheSize, maxCachedBufferCapacity, directArena);
            //为一个线程创建了私有内存池，内存池引用了directArena，所以要为这个PoolArena的引用计数加1
            directArena.numThreadCaches.getAndIncrement();
        } else {
            //directArena为0则设置所有缓存为null
            tinySubPageDirectCaches = null;
            smallSubPageDirectCaches = null;
            normalDirectCaches = null;
            numShiftsNormalDirect = -1;
        }
        if (heapArena != null) {
            //这里使用的就是堆内存，逻辑和上面差不多就不再详细注释了
            tinySubPageHeapCaches = createSubPageCaches(
                    tinyCacheSize, PoolArena.numTinySubpagePools, PoolArena.SizeClass.Tiny);
            smallSubPageHeapCaches = createSubPageCaches(
                    smallCacheSize, heapArena.numSmallSubpagePools, PoolArena.SizeClass.Small);

            numShiftsNormalHeap = log2(heapArena.pageSize);
            normalHeapCaches = createNormalCaches(
                    normalCacheSize, maxCachedBufferCapacity, heapArena);

            heapArena.numThreadCaches.getAndIncrement();
        } else {
            tinySubPageHeapCaches = null;
            smallSubPageHeapCaches = null;
            normalHeapCaches = null;
            numShiftsNormalHeap = -1;
        }
        if ((tinySubPageDirectCaches != null || smallSubPageDirectCaches != null || normalDirectCaches != null
             || tinySubPageHeapCaches != null || smallSubPageHeapCaches != null || normalHeapCaches != null)
            && freeSweepAllocationThreshold < 1) {
            throw new IllegalArgumentException("freeSweepAllocationThreshold: "
                                               + freeSweepAllocationThreshold + " (expected: > 0)");
        }
    }

    /**
     * @Author: ytrue
     * @Description:创建缓存小于8KB内存的容器数组，这个方法是tiny和small共用的，传进来的参数不同，创建的数组和队列的容量也就不同
     * cacheSize的值为512或256
     * numCaches的值为32或4
     * sizeClass的值为Tiny或Small，是个枚举对象
     * 这些属性都是从PooledByteBufAllocator中就确定好的
     * 这里就不再赘述了，大家可以按照逻辑看看参数是怎么被传送至此的
     */
    private static <T> MemoryRegionCache<T>[] createSubPageCaches(
            int cacheSize, int numCaches, PoolArena.SizeClass sizeClass) {
        if (cacheSize > 0 && numCaches > 0) {
            //下面就是创建了一个MemoryRegionCache数组，numCaches是数组的容量
            //其实数组的每一个索引位置存储的都是一个SubPageMemoryRegionCache对象，该对象内部有一个队列
            //这个队列就是真正缓存内存地址的容器 cacheSize就是该队列的容量
            @SuppressWarnings("unchecked")
            MemoryRegionCache<T>[] cache = new MemoryRegionCache[numCaches];
            for (int i = 0; i < cache.length; i++) {
                // TODO: maybe use cacheSize / cache.length
                //在这里为数组的每一个位置创建了一个队列
                cache[i] = new SubPageMemoryRegionCache<T>(cacheSize, sizeClass);
            }
            return cache;
        } else {
            return null;
        }
    }


    /**
     * @Author: ytrue
     * @Description:创建缓存大于8KB内存的容器数组
     * cacheSize的值为64，为真正缓存内存地址的队列的容量
     * maxCachedBufferCapacity为32KB
     * 因为内存池中可以被缓存的最大内存值为32kb，超过32就不再被缓存
     */
    private static <T> MemoryRegionCache<T>[] createNormalCaches(
            int cacheSize, int maxCachedBufferCapacity, PoolArena<T> area) {
        if (cacheSize > 0 && maxCachedBufferCapacity > 0) {
            int max = Math.min(area.chunkSize, maxCachedBufferCapacity);
            //计算出缓存normal内存块的数组的容量大小
            //这里得到的是log2(32KB/8)+1的值
            //最后得到的就是3
            //其实就是只缓存8K,16K,32K大小的内存
            int arraySize = Math.max(1, log2(max / area.pageSize) + 1);
            //创建缓存内存的数组容器
            @SuppressWarnings("unchecked")
            MemoryRegionCache<T>[] cache = new MemoryRegionCache[arraySize];
            for (int i = 0; i < cache.length; i++) {
                //为数组的每个索引位置创建真正缓存内存地址的队列
                cache[i] = new NormalMemoryRegionCache<T>(cacheSize);
            }
            return cache;
        } else {
            return null;
        }
    }

    private static int log2(int val) {
        int res = 0;
        while (val > 1) {
            val >>= 1;
            res++;
        }
        return res;
    }

    /**
     * @Author: ytrue
     * @Description:从内存池中分配tiny大小内存的方法
     */
    boolean allocateTiny(PoolArena<?> area, PooledByteBuf<?> buf, int reqCapacity, int normCapacity) {
        return allocate(cacheForTiny(area, normCapacity), buf, reqCapacity);
    }

    /**
     * @Author: ytrue
     * @Description:从内存池中分配small大小内存的方法
     */
    boolean allocateSmall(PoolArena<?> area, PooledByteBuf<?> buf, int reqCapacity, int normCapacity) {
        return allocate(cacheForSmall(area, normCapacity), buf, reqCapacity);
    }

    /**
     * @Author: ytrue
     * @Description:从内存池中分配normal大小内存的方法
     */
    boolean allocateNormal(PoolArena<?> area, PooledByteBuf<?> buf, int reqCapacity, int normCapacity) {
        return allocate(cacheForNormal(area, normCapacity), buf, reqCapacity);
    }


    /**
     * @Author: ytrue
     * @Description:真正从内存池中分配内存的方法
     * 其实就是从真正存储内存的MemoryRegionCache的队列中分配内存
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean allocate(MemoryRegionCache<?> cache, PooledByteBuf buf, int reqCapacity) {
        if (cache == null) {
            //该对象为null，自然也就不会有队列，直接返回即可
            return false;
        }
        //从队列中分配内存
        boolean allocated = cache.allocate(buf, reqCapacity);
        //分配了之后，要把该内存池分配内存的引用计数加1
        //并且判断该内存池的分配次数达到阈值了，就把分配次数置为0，然后清理一次内存池，防止内存无效占用
        if (++ allocations >= freeSweepAllocationThreshold) {
            allocations = 0;
            //清理内存池
            trim();
        }
        return allocated;
    }

    /**
     * @Author: ytrue
     * @Description:把内存添加到内存池中
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    boolean add(PoolArena<?> area, PoolChunk chunk, ByteBuffer nioBuffer,
                long handle, int normCapacity, PoolArena.SizeClass sizeClass) {
        //得到代缓存的内存要真正存放的那个MemoryRegionCache对象
        MemoryRegionCache<?> cache = cache(area, normCapacity, sizeClass);
        if (cache == null) {
            return false;
        }
        //把内存放进该对象中的队列中
        return cache.add(chunk, nioBuffer, handle);
    }

    /**
     * @Author: ytrue
     * @Description:找到要把待缓存的内存存放到哪个数组的哪个索引对应的MemoryRegionCache对象中
     */
    private MemoryRegionCache<?> cache(PoolArena<?> area, int normCapacity, PoolArena.SizeClass sizeClass) {
        switch (sizeClass) {
            case Normal:
                return cacheForNormal(area, normCapacity);
            case Small:
                return cacheForSmall(area, normCapacity);
            case Tiny:
                return cacheForTiny(area, normCapacity);
            default:
                throw new Error();
        }
    }

    /// TODO: In the future when we move to Java9+ we should use java.lang.ref.Cleaner.
    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            free(true);
        }
    }

    /**
     * @Author: ytrue
     * @Description:释放内存并且垃圾回收包装内存对象的方法
     */
    void free(boolean finalizer) {
        //原子类更新状态
        if (freed.compareAndSet(false, true)) {
            int numFreed = free(tinySubPageDirectCaches, finalizer) +
                           free(smallSubPageDirectCaches, finalizer) +
                           free(normalDirectCaches, finalizer) +
                           free(tinySubPageHeapCaches, finalizer) +
                           free(smallSubPageHeapCaches, finalizer) +
                           free(normalHeapCaches, finalizer);
            if (numFreed > 0 && logger.isDebugEnabled()) {
                logger.debug("Freed {} thread-local buffer(s) from thread: {}", numFreed,
                        Thread.currentThread().getName());
            }
            if (directArena != null) {
                directArena.numThreadCaches.getAndDecrement();
            }
            if (heapArena != null) {
                heapArena.numThreadCaches.getAndDecrement();
            }
        }
    }

    /**
     * @Author: ytrue
     * @Description:释放MemoryRegionCache数组中内存的方法
     */
    private static int free(MemoryRegionCache<?>[] caches, boolean finalizer) {
        if (caches == null) {
            return 0;
        }
        int numFreed = 0;
        //遍历该数组，取出每一个MemoryRegionCache对象
        for (MemoryRegionCache<?> c: caches) {
            numFreed += free(c, finalizer);
        }
        return numFreed;
    }

    /**
     * @Author: ytrue
     * @Description:释放MemoryRegionCache对象中内存的方法
     */
    private static int free(MemoryRegionCache<?> cache, boolean finalizer) {
        if (cache == null) {
            return 0;
        }
        return cache.free(finalizer);
    }

    /**
     * @Author: ytrue
     * @Description:清理内存池的方法，这里可以看到，所谓清理内存池，实际上就是把内存池中的各个数组清理一遍
     */
    void trim() {
        trim(tinySubPageDirectCaches);
        trim(smallSubPageDirectCaches);
        trim(normalDirectCaches);
        trim(tinySubPageHeapCaches);
        trim(smallSubPageHeapCaches);
        trim(normalHeapCaches);
    }

    /**
     * @Author: ytrue
     * @Description:清理内存池内数组的方法
     */
    private static void trim(MemoryRegionCache<?>[] caches) {
        if (caches == null) {
            return;
        }
        //遍历这个数组，循环清理数组中的每一个位置的MemoryRegionCache对象
        for (MemoryRegionCache<?> c: caches) {
            trim(c);
        }
    }

    /**
     * @Author: ytrue
     * @Description:清理MemoryRegionCache对象内部的内存
     */
    private static void trim(MemoryRegionCache<?> cache) {
        if (cache == null) {
            return;
        }
        cache.trim();
    }

    /**
     * @Author: ytrue
     * @Description:计算该内存要从缓存的数组中的那个下标索引对应的MemoryRegionCache中分配内存
     * 下面两个方法同理，就不再加注释了
     */
    private MemoryRegionCache<?> cacheForTiny(PoolArena<?> area, int normCapacity) {
        //根据要分配的内存计算下标索引
        int idx = PoolArena.tinyIdx(normCapacity);
        if (area.isDirect()) {
            //如果使用的是直接内存，就从直接内存的缓存中分配
            return cache(tinySubPageDirectCaches, idx);
        }
        return cache(tinySubPageHeapCaches, idx);
    }

    private MemoryRegionCache<?> cacheForSmall(PoolArena<?> area, int normCapacity) {
        int idx = PoolArena.smallIdx(normCapacity);
        if (area.isDirect()) {
            return cache(smallSubPageDirectCaches, idx);
        }
        return cache(smallSubPageHeapCaches, idx);
    }

    private MemoryRegionCache<?> cacheForNormal(PoolArena<?> area, int normCapacity) {
        if (area.isDirect()) {
            int idx = log2(normCapacity >> numShiftsNormalDirect);
            return cache(normalDirectCaches, idx);
        }
        int idx = log2(normCapacity >> numShiftsNormalHeap);
        return cache(normalHeapCaches, idx);
    }

    /**
     * @Author: ytrue
     * @Description:从MemoryRegionCache数组中找到具体要分配内存的那个MemoryRegionCache对象
     */
    private static <T> MemoryRegionCache<T> cache(MemoryRegionCache<T>[] cache, int idx) {
        if (cache == null || idx > cache.length - 1) {
            return null;
        }
        //返回该对象
        return cache[idx];
    }

    /**
     * @Author: ytrue
     * @Description:SubPageMemoryRegionCache内部类
     */
    private static final class SubPageMemoryRegionCache<T> extends MemoryRegionCache<T> {
        SubPageMemoryRegionCache(int size, PoolArena.SizeClass sizeClass) {
            super(size, sizeClass);
        }

        @Override
        protected void initBuf(
                PoolChunk<T> chunk, ByteBuffer nioBuffer, long handle, PooledByteBuf<T> buf, int reqCapacity) {
            chunk.initBufWithSubpage(buf, nioBuffer, handle, reqCapacity);
        }
    }

    /**
     * @Author: ytrue
     * @Description:NormalMemoryRegionCache内部类
     */
    private static final class NormalMemoryRegionCache<T> extends MemoryRegionCache<T> {
        NormalMemoryRegionCache(int size) {
            super(size, PoolArena.SizeClass.Normal);
        }

        @Override
        protected void initBuf(
                PoolChunk<T> chunk, ByteBuffer nioBuffer, long handle, PooledByteBuf<T> buf, int reqCapacity) {
            chunk.initBuf(buf, nioBuffer, handle, reqCapacity);
        }
    }

    /**
     * @Author: ytrue
     * @Description:MemoryRegionCache内部类
     */
    private abstract static class MemoryRegionCache<T> {
        //queue队列的容量大小
        private final int size;
        //存放内存的队列，内存被entry对象包装了，所以该队列存放的就是一个个entry对象
        //该队列在构造器中被初始化了，使用的是JCTools中高性能队列，多生产者单消费者这队列
        //为什么这里要用这个队列，大家一定要弄清楚，就像对象池一样，从内存池中申请内存的时候，都是从每个线程
        //的私有内存池中申请的，但是释放的时候并不是这样，可能是其他线程帮忙释放内存
        //这时候大家就要找到释放内存的起点，释放内存其实就是byteBuf.release()这个方法
        //然后会调用到PoolByteBuf的deallocate方法进行释放，这个方法中会把ByteBuf归还给对象池
        //然后把内存释放到PoolByteBuf内部持有的内存池中，这就意味着这块内存对应的内存池是确定的，但是释放内存的线程不一定是内存池
        //对应的线程，所以就可能出现多个其他线程帮助释放内存的情况，但是分配内存的时候只有内存池所属的线程
        //来申请内存，所以才要用这个多生产者单消费者队列
        private final Queue<Entry<T>> queue;
        //缓存的内存的类型
        private final PoolArena.SizeClass sizeClass;
        //记录当前的队列分配了多少次内存
        private int allocations;

        /**
         * @Author: ytrue
         * @Description:构造器方法
         */
        MemoryRegionCache(int size, PoolArena.SizeClass sizeClass) {
            this.size = MathUtil.safeFindNextPositivePowerOfTwo(size);
            //这里可以看到，使用的是高性能无锁队列
            queue = PlatformDependent.newFixedMpscQueue(this.size);
            this.sizeClass = sizeClass;
        }

        /**
         * @Author: ytrue
         * @Description:初始化ByteBuf的方法，就是把内存交给PooledByteBuf使用
         * 这里定义为抽象方法，是因为分配8KB以上的内存和分配8KB以下的内存是不一样的，内存偏移量的计算方式不同，所以交给
         * 不同的子类去实现
         */
        protected abstract void initBuf(PoolChunk<T> chunk, ByteBuffer nioBuffer, long handle,
                                        PooledByteBuf<T> buf, int reqCapacity);

        /**
         * @Author: ytrue
         * @Description:把要缓存的内存添加到队列中，如果添加失败说明队列满了，这个时候就直接把内存释放掉
         */
        @SuppressWarnings("unchecked")
        public final boolean add(PoolChunk<T> chunk, ByteBuffer nioBuffer, long handle) {
            //从对象池中得到一个entry对象
            //并且把要释放的内存和ByteBuffer放到该对象中
            //注意啊，如果添加内存池失败了，才会考虑将这块内存归还给Chunk或者PoolSubpage，这个时候ByteBuffer对象
            //就会被缓存到PoolArena中的cachedNioBuffers队列中了
            Entry<T> entry = newEntry(chunk, nioBuffer, handle);
            boolean queued = queue.offer(entry);
            if (!queued) {
                //添加失败了，就把entry对象回收
                entry.recycle();
            }
            return queued;
        }

        /**
         * @Author: ytrue
         * @Description:分配内存的方法
         */
        public final boolean allocate(PooledByteBuf<T> buf, int reqCapacity) {
            //从队列中取出一个entry对象
            Entry<T> entry = queue.poll();
            if (entry == null) {
                return false;
            }
            //用entry对象包装的内存地址初始化PooledByteBuf对象
            initBuf(entry.chunk, entry.nioBuffer, entry.handle, buf, reqCapacity);
            //用完之后释放entry对象到对象池
            entry.recycle();
            //分配的引用计数加一
            ++ allocations;
            return true;
        }

        /**
         * @Author: ytrue
         * @Description:释放队列中内存的方法
         */
        public final int free(boolean finalizer) {
            return free(Integer.MAX_VALUE, finalizer);
        }

        /**
         * @Author: ytrue
         * @Description:释放队列中缓存的内存
         * max就是可能剩下的还未分配的内存的块数
         */
        private int free(int max, boolean finalizer) {
            int numFreed = 0;
            //根据max遍历队列
            for (; numFreed < max; numFreed++) {
                //取出entry对象
                Entry<T> entry = queue.poll();
                if (entry != null) {
                    //释放entry对象，释放对象的同时，其实也就把内存归还给Chunk内存块了
                    freeEntry(entry, finalizer);
                } else {
                    return numFreed;
                }
            }
            return numFreed;
        }

        /**
         * @Author: ytrue
         * @Description:清理MemoryRegionCache对象内部存放的内存，实际上就是释放了这些内存
         */
        public final void trim() {
            //size是该队列的容量
            //allocations是该队列分配了多少次内存
            int free = size - allocations;
            //清理的时候会把allocations重置为0
            allocations = 0;
            //free大于0，说明该队列分配了很多次内存，但是仍然没有把内存全分配完
            //这说明队列中缓存的内存使用的次数并不多
            //既然这样，就可以释放一部分，防止内存无效占用
            if (free > 0) {
                //这时候就要清理一次这个队列
                free(free, false);
            }
        }

        /**
         * @Author: ytrue
         * @Description:释放entry对象，把entry包装的内存归还给Chunk内存块
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        private  void freeEntry(Entry entry, boolean finalizer) {
            PoolChunk chunk = entry.chunk;
            long handle = entry.handle;
            ByteBuffer nioBuffer = entry.nioBuffer;
            if (!finalizer) {
                //这里的的finalizer是最开始外面传进来的，默认为false，意思是该entry对象不会被垃圾回收
                //要放到对象池中
                entry.recycle();
            }
            //把内存地址归还给Chunk内存块中
            //ByteBuffer也缓存到PoolChuank对象的队列中
            chunk.arena.freeChunk(chunk, handle, sizeClass, nioBuffer, finalizer);
        }


        /**
         * @Author: ytrue
         * @Description:Entry内部类，就是这个类包装了要被缓存的内存
         * 这个类中持有chunk内存块和ByteBuffer对象
         */
        static final class Entry<T> {
            final Recycler.Handle<Entry<?>> recyclerHandle;
            PoolChunk<T> chunk;
            ByteBuffer nioBuffer;
            long handle = -1;

            Entry(Recycler.Handle<Entry<?>> recyclerHandle) {
                this.recyclerHandle = recyclerHandle;
            }

            void recycle() {
                chunk = null;
                nioBuffer = null;
                handle = -1;
                recyclerHandle.recycle(this);
            }
        }

        @SuppressWarnings("rawtypes")
        private static Entry newEntry(PoolChunk<?> chunk, ByteBuffer nioBuffer, long handle) {
            Entry entry = RECYCLER.get();
            entry.chunk = chunk;
            entry.nioBuffer = nioBuffer;
            entry.handle = handle;
            return entry;
        }

        /**
         * @Author: ytrue
         * @Description:该对象的对象池
         */
        @SuppressWarnings("rawtypes")
        private static final Recycler<Entry> RECYCLER = new Recycler<Entry>() {
            @SuppressWarnings("unchecked")
            @Override
            protected Entry newObject(Handle<Entry> handle) {
                return new Entry(handle);
            }
        };
    }
}
