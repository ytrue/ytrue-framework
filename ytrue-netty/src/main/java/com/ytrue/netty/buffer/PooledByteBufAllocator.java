package com.ytrue.netty.buffer;

import com.ytrue.netty.util.NettyRuntime;
import com.ytrue.netty.util.concurrent.EventExecutor;
import com.ytrue.netty.util.concurrent.FastThreadLocal;
import com.ytrue.netty.util.concurrent.FastThreadLocalThread;
import com.ytrue.netty.util.internal.PlatformDependent;
import com.ytrue.netty.util.internal.StringUtil;
import com.ytrue.netty.util.internal.SystemPropertyUtil;
import com.ytrue.netty.util.internal.ThreadExecutorMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.ytrue.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * @author ytrue
 * @date 2023-08-07 11:38
 * @description 该类就是分配池化内存的内存分配器
 */
public class PooledByteBufAllocator extends AbstractByteBufAllocator implements ByteBufAllocatorMetricProvider {

    private static final Logger logger = LoggerFactory.getLogger(PooledByteBufAllocator.class);

    //堆类型的ARENA的数量，通常情况下默认初始值是CPU核数乘以2
    private static final int DEFAULT_NUM_HEAP_ARENA;
    //直接内存类型，也就是direc类型的ARENA的数量，通常情况下默认初始值是CPU核数乘以2
    private static final int DEFAULT_NUM_DIRECT_ARENA;
    //Page页的默认大小，值为8KB
    private static final int DEFAULT_PAGE_SIZE;
    //二叉树的高度，默认值为11
    private static final int DEFAULT_MAX_ORDER;
    //缓存在内存池中的tiny类型的内存个数，默认值为512，实际上就是内存池中缓存队列的容量，下面同理
    private static final int DEFAULT_TINY_CACHE_SIZE;
    //缓存在内存池中的small类型的内存个数，默认为值为256
    private static final int DEFAULT_SMALL_CACHE_SIZE;
    //缓存在内存池中的normal类型的内存个数，默认为值为64
    private static final int DEFAULT_NORMAL_CACHE_SIZE;
    //内存池中可以被缓存的最大内存值为32kb，如果一个用户申请了一块超过32kb的内存，这块内存用完后就会直接返回给Chunk，
    //而不是放入内存池
    private static final int DEFAULT_MAX_CACHED_BUFFER_CAPACITY;
    //内存池释放内存时的阈值，默认值为8192，表示内存池分配的内存次数超过了这个值，内存池中内存就要被释放
    //这么做其实是为了不让内存池占用太多内存，如果有些内存在内存池中并没有被频繁利用，就应该释放供其他地方使用
    private static final int DEFAULT_CACHE_TRIM_INTERVAL;
    //这个值的作用是定时释放内存池，默认值为0，默认不开启这种做法
    private static final long DEFAULT_CACHE_TRIM_INTERVAL_MILLIS;
    //是否为所有线程都分配一个内存池，默认为true。之前我们学习了对象池，知道每一个线程实际上都有一个私有的对象池
    //内存池也是如此，通过FastThreadLocal实现的
    private static final boolean DEFAULT_USE_CACHE_FOR_ALL_THREADS;
    private static final int DEFAULT_DIRECT_MEMORY_CACHE_ALIGNMENT;
    //缓存的最大ByteBuff数量，默认为1023
    static final int DEFAULT_MAX_CACHED_BYTEBUFFERS_PER_CHUNK;
    //该值是一个校验属性，用来和pageSize值做比较，规定pageSize不能小于4096
    private static final int MIN_PAGE_SIZE = 4096;
    //该值是用来校验Chunk的，规定Chunk不能超过该值
    private static final int MAX_CHUNK_SIZE = (int) (((long) Integer.MAX_VALUE + 1) / 2);

    /**
     * @Author: ytrue
     * @Description:和内存池有关的异步任务，用来释放内存池中内存的，但一般情况下这个任务不会被执行，在创建内存池的地方
     * 有详细逻辑
     */
    private final Runnable trimTask = new Runnable() {
        @Override
        public void run() {
            PooledByteBufAllocator.this.trimCurrentThreadCache();
        }
    };

    /**
     * @Author: ytrue
     * @Description:静态代码块中几乎都是给上面的成员变量赋值的方法，都是通过SystemPropertyUtil来实现的
     * 大家可以自己点一点看一下，代码虽然多，但一点也不难，我就不加那么多注释了
     */
    static {
        int defaultPageSize = SystemPropertyUtil.getInt("io.netty.allocator.pageSize", 8192);
        Throwable pageSizeFallbackCause = null;
        try {
            validateAndCalculatePageShifts(defaultPageSize);
        } catch (Throwable t) {
            pageSizeFallbackCause = t;
            defaultPageSize = 8192;
        }
        DEFAULT_PAGE_SIZE = defaultPageSize;

        int defaultMaxOrder = SystemPropertyUtil.getInt("io.netty.allocator.maxOrder", 11);
        Throwable maxOrderFallbackCause = null;
        try {
            validateAndCalculateChunkSize(DEFAULT_PAGE_SIZE, defaultMaxOrder);
        } catch (Throwable t) {
            maxOrderFallbackCause = t;
            defaultMaxOrder = 11;
        }
        DEFAULT_MAX_ORDER = defaultMaxOrder;

        final Runtime runtime = Runtime.getRuntime();
        //得到CPU的核数乘以2的值
        final int defaultMinNumArena = NettyRuntime.availableProcessors() * 2;
        final int defaultChunkSize = DEFAULT_PAGE_SIZE << DEFAULT_MAX_ORDER;
        DEFAULT_NUM_HEAP_ARENA = Math.max(0,
                SystemPropertyUtil.getInt(
                        "io.netty.allocator.numHeapArenas",
                        (int) Math.min(
                                defaultMinNumArena,
                                runtime.maxMemory() / defaultChunkSize / 2 / 3)));
        DEFAULT_NUM_DIRECT_ARENA = Math.max(0,
                SystemPropertyUtil.getInt(
                        "io.netty.allocator.numDirectArenas",
                        (int) Math.min(
                                defaultMinNumArena,
                                PlatformDependent.maxDirectMemory() / defaultChunkSize / 2 / 3)));


        DEFAULT_TINY_CACHE_SIZE = SystemPropertyUtil.getInt("io.netty.allocator.tinyCacheSize", 512);
        DEFAULT_SMALL_CACHE_SIZE = SystemPropertyUtil.getInt("io.netty.allocator.smallCacheSize", 256);
        DEFAULT_NORMAL_CACHE_SIZE = SystemPropertyUtil.getInt("io.netty.allocator.normalCacheSize", 64);

        DEFAULT_MAX_CACHED_BUFFER_CAPACITY = SystemPropertyUtil.getInt(
                "io.netty.allocator.maxCachedBufferCapacity", 32 * 1024);

        DEFAULT_CACHE_TRIM_INTERVAL = SystemPropertyUtil.getInt(
                "io.netty.allocator.cacheTrimInterval", 8192);

        DEFAULT_CACHE_TRIM_INTERVAL_MILLIS = SystemPropertyUtil.getLong(
                "io.netty.allocation.cacheTrimIntervalMillis", 0);

        DEFAULT_USE_CACHE_FOR_ALL_THREADS = SystemPropertyUtil.getBoolean(
                "io.netty.allocator.useCacheForAllThreads", true);

        DEFAULT_DIRECT_MEMORY_CACHE_ALIGNMENT = SystemPropertyUtil.getInt(
                "io.netty.allocator.directMemoryCacheAlignment", 0);

        DEFAULT_MAX_CACHED_BYTEBUFFERS_PER_CHUNK = SystemPropertyUtil.getInt(
                "io.netty.allocator.maxCachedByteBuffersPerChunk", 1023);

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.allocator.numHeapArenas: {}", DEFAULT_NUM_HEAP_ARENA);
            logger.debug("-Dio.netty.allocator.numDirectArenas: {}", DEFAULT_NUM_DIRECT_ARENA);
            if (pageSizeFallbackCause == null) {
                logger.debug("-Dio.netty.allocator.pageSize: {}", DEFAULT_PAGE_SIZE);
            } else {
                logger.debug("-Dio.netty.allocator.pageSize: {}", DEFAULT_PAGE_SIZE, pageSizeFallbackCause);
            }
            if (maxOrderFallbackCause == null) {
                logger.debug("-Dio.netty.allocator.maxOrder: {}", DEFAULT_MAX_ORDER);
            } else {
                logger.debug("-Dio.netty.allocator.maxOrder: {}", DEFAULT_MAX_ORDER, maxOrderFallbackCause);
            }
            logger.debug("-Dio.netty.allocator.chunkSize: {}", DEFAULT_PAGE_SIZE << DEFAULT_MAX_ORDER);
            logger.debug("-Dio.netty.allocator.tinyCacheSize: {}", DEFAULT_TINY_CACHE_SIZE);
            logger.debug("-Dio.netty.allocator.smallCacheSize: {}", DEFAULT_SMALL_CACHE_SIZE);
            logger.debug("-Dio.netty.allocator.normalCacheSize: {}", DEFAULT_NORMAL_CACHE_SIZE);
            logger.debug("-Dio.netty.allocator.maxCachedBufferCapacity: {}", DEFAULT_MAX_CACHED_BUFFER_CAPACITY);
            logger.debug("-Dio.netty.allocator.cacheTrimInterval: {}", DEFAULT_CACHE_TRIM_INTERVAL);
            logger.debug("-Dio.netty.allocator.cacheTrimIntervalMillis: {}", DEFAULT_CACHE_TRIM_INTERVAL_MILLIS);
            logger.debug("-Dio.netty.allocator.useCacheForAllThreads: {}", DEFAULT_USE_CACHE_FOR_ALL_THREADS);
            logger.debug("-Dio.netty.allocator.maxCachedByteBuffersPerChunk: {}",
                    DEFAULT_MAX_CACHED_BYTEBUFFERS_PER_CHUNK);
        }
    }

    /**
     * @Author: ytrue
     * @Description:池化的内存分配器在这里创建了
     */
    public static final PooledByteBufAllocator DEFAULT =
            new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());

    //DEFAULT_NUM_HEAP_ARENA这个值就是数组的初始化容量，这个数组中存储的都是基于堆内存的PoolArena
    private final PoolArena<byte[]>[] heapArenas;
    //DEFAULT_NUM_DIRECT_ARENA这个值就是数组的初始化容量，这个数组中存储的都是基于直接内存的PoolArena
    private final PoolArena<ByteBuffer>[] directArenas;
    //DEFAULT_TINY_CACHE_SIZE会赋值给该值
    private final int tinyCacheSize;
    //DEFAULT_SMALL_CACHE_SIZE会赋值给该值
    private final int smallCacheSize;
    //DEFAULT_NORMAL_CACHE_SIZE会赋值给该值
    private final int normalCacheSize;
    private final List<PoolArenaMetric> heapArenaMetrics;
    private final List<PoolArenaMetric> directArenaMetrics;
    //这就是每个线程私有的内存池的入口，本身是一个FastThreadLocal
    private final PoolThreadLocalCache threadCache;
    //每一个Chunk的大小
    private final int chunkSize;
    private final PooledByteBufAllocatorMetric metric;

    public PooledByteBufAllocator() {
        this(false);
    }

    @SuppressWarnings("deprecation")
    //是不是要采用直接内存
    public PooledByteBufAllocator(boolean preferDirect) {
        this(preferDirect, DEFAULT_NUM_HEAP_ARENA, DEFAULT_NUM_DIRECT_ARENA, DEFAULT_PAGE_SIZE, DEFAULT_MAX_ORDER);
    }

    @SuppressWarnings("deprecation")
    public PooledByteBufAllocator(int nHeapArena, int nDirectArena, int pageSize, int maxOrder) {
        this(false, nHeapArena, nDirectArena, pageSize, maxOrder);
    }


    @Deprecated
    public PooledByteBufAllocator(boolean preferDirect, int nHeapArena, int nDirectArena, int pageSize, int maxOrder) {
        this(preferDirect, nHeapArena, nDirectArena, pageSize, maxOrder,
                DEFAULT_TINY_CACHE_SIZE, DEFAULT_SMALL_CACHE_SIZE, DEFAULT_NORMAL_CACHE_SIZE);
    }


    @Deprecated
    public PooledByteBufAllocator(boolean preferDirect, int nHeapArena, int nDirectArena, int pageSize, int maxOrder,
                                  int tinyCacheSize, int smallCacheSize, int normalCacheSize) {
        this(preferDirect, nHeapArena, nDirectArena, pageSize, maxOrder, tinyCacheSize, smallCacheSize,
                normalCacheSize, DEFAULT_USE_CACHE_FOR_ALL_THREADS, DEFAULT_DIRECT_MEMORY_CACHE_ALIGNMENT);
    }

    public PooledByteBufAllocator(boolean preferDirect, int nHeapArena,
                                  int nDirectArena, int pageSize, int maxOrder, int tinyCacheSize,
                                  int smallCacheSize, int normalCacheSize,
                                  boolean useCacheForAllThreads) {
        this(preferDirect, nHeapArena, nDirectArena, pageSize, maxOrder,
                tinyCacheSize, smallCacheSize, normalCacheSize,
                useCacheForAllThreads, DEFAULT_DIRECT_MEMORY_CACHE_ALIGNMENT);
    }

    /**
     * @Author: ytrue
     * @Description:池化的内存分配器的核心构造函数，大家可以仔细看看，这个构造函数中的所有参数，用的都是该类中的哪些成员变量
     */
    public PooledByteBufAllocator(boolean preferDirect, int nHeapArena, int nDirectArena, int pageSize, int maxOrder,
                                  int tinyCacheSize, int smallCacheSize, int normalCacheSize,
                                  boolean useCacheForAllThreads, int directMemoryCacheAlignment) {
        //这里要把这个preferDirect传递到父类的构造函数中，是因为创建一个buffer时，调用的都是抽象父类中的方法，而这个属性就决定了
        //是否可以创建池化的内存buffer，否则只能创建堆内存的buffer。具体逻辑可以从父类中查看
        super(preferDirect);
        //在这里创建了一个FastTreadLocal，其实就可以把这个属性当成每个线程内存池的入口
        threadCache = new PoolThreadLocalCache(useCacheForAllThreads);
        //下面就是给这三个内存池要用到的属性赋值了
        this.tinyCacheSize = tinyCacheSize;
        this.smallCacheSize = smallCacheSize;
        this.normalCacheSize = normalCacheSize;
        //给Chunk赋值
        chunkSize = validateAndCalculateChunkSize(pageSize, maxOrder);
        //下面就是做一些参数校验
        checkPositiveOrZero(nHeapArena, "nHeapArena");
        checkPositiveOrZero(nDirectArena, "nDirectArena");

        checkPositiveOrZero(directMemoryCacheAlignment, "directMemoryCacheAlignment");
        if (directMemoryCacheAlignment > 0 && !isDirectMemoryCacheAlignmentSupported()) {
            throw new IllegalArgumentException("directMemoryCacheAlignment is not supported");
        }

        if ((directMemoryCacheAlignment & -directMemoryCacheAlignment) != directMemoryCacheAlignment) {
            throw new IllegalArgumentException("directMemoryCacheAlignment: "
                                               + directMemoryCacheAlignment + " (expected: power of two)");
        }
        //这个在PoolArena中讲解
        int pageShifts = validateAndCalculatePageShifts(pageSize);

        if (nHeapArena > 0) {
            //这里创建了堆内存空间数组，nHeapArena就是数组的初始容量，容量默认是CPU的个数乘以2。
            //这里之所以这么做，是因为我们当初创建workergroup的时候，默认的线程数量就是CPU个数乘以2
            //而在内存分配中，每一个线程都会分到一个PoolArena，一个PoolArena可以分给多个线程
            //所以，为了让每个线程至少能分配一个独立的PoolArena，PoolArena的个数要和线程数对等。
            //当然，创建的workergroup线程可能会更多，这时候，在内存池初始化的时候，就会筛检被其他线程利用最少的PoolArena
            //交给创建内存池的线程使用，这么做就会显著降低并发程度，如果很多个线程都引用了同一个PoolArena，那并发度就会很高了
            heapArenas = newArenaArray(nHeapArena);
            List<PoolArenaMetric> metrics = new ArrayList<PoolArenaMetric>(heapArenas.length);
            //给heapArenas数组的每一个下标赋值
            for (int i = 0; i < heapArenas.length; i ++) {
                //创建基于堆内存的HeapArena对象，这里大家可以仔细看看构造器中的参数都是上面的那几个成员变量
                PoolArena.HeapArena arena = new PoolArena.HeapArena(this,
                        pageSize, maxOrder, pageShifts, chunkSize,
                        directMemoryCacheAlignment);
                //给数组下标赋值
                heapArenas[i] = arena;
                metrics.add(arena);
            }
            heapArenaMetrics = Collections.unmodifiableList(metrics);
        } else {
            heapArenas = null;
            heapArenaMetrics = Collections.emptyList();
        }
        //这里和上面一样，只不过就是堆内存换成了直接内存
        if (nDirectArena > 0) {
            directArenas = newArenaArray(nDirectArena);
            List<PoolArenaMetric> metrics = new ArrayList<PoolArenaMetric>(directArenas.length);
            for (int i = 0; i < directArenas.length; i ++) {
                PoolArena.DirectArena arena = new PoolArena.DirectArena(
                        this, pageSize, maxOrder, pageShifts, chunkSize, directMemoryCacheAlignment);
                directArenas[i] = arena;
                metrics.add(arena);
            }
            directArenaMetrics = Collections.unmodifiableList(metrics);
        } else {
            directArenas = null;
            directArenaMetrics = Collections.emptyList();
        }
        metric = new PooledByteBufAllocatorMetric(this);
    }

    @SuppressWarnings("unchecked")
    private static <T> PoolArena<T>[] newArenaArray(int size) {
        return new PoolArena[size];
    }

    private static int validateAndCalculatePageShifts(int pageSize) {
        if (pageSize < MIN_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize: " + pageSize + " (expected: " + MIN_PAGE_SIZE + ")");
        }

        if ((pageSize & pageSize - 1) != 0) {
            throw new IllegalArgumentException("pageSize: " + pageSize + " (expected: power of 2)");
        }

        return Integer.SIZE - 1 - Integer.numberOfLeadingZeros(pageSize);
    }

    private static int validateAndCalculateChunkSize(int pageSize, int maxOrder) {
        if (maxOrder > 14) {
            throw new IllegalArgumentException("maxOrder: " + maxOrder + " (expected: 0-14)");
        }

        // Ensure the resulting chunkSize does not overflow.
        int chunkSize = pageSize;
        for (int i = maxOrder; i > 0; i --) {
            if (chunkSize > MAX_CHUNK_SIZE / 2) {
                throw new IllegalArgumentException(String.format(
                        "pageSize (%d) << maxOrder (%d) must not exceed %d", pageSize, maxOrder, MAX_CHUNK_SIZE));
            }
            chunkSize <<= 1;
        }
        return chunkSize;
    }

    //该方法的方法体也暂时注释掉，因为没有引入UnpooledUnsafeHeapByteBuf和UnpooledHeapByteBuf类。
    @Override
    protected ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity) {
//        PoolThreadCache cache = threadCache.get();
//        PoolArena<byte[]> heapArena = cache.heapArena;
//
//        final ByteBuf buf;
//        if (heapArena != null) {
//            buf = heapArena.allocate(cache, initialCapacity, maxCapacity);
//        } else {
//            buf = PlatformDependent.hasUnsafe() ?
//                    new UnpooledUnsafeHeapByteBuf(this, initialCapacity, maxCapacity) :
//                    new UnpooledHeapByteBuf(this, initialCapacity, maxCapacity);
//        }
//
//        return toLeakAwareBuffer(buf);
        return null;
    }


    /**
     * @Author: ytrue
     * @Description:创建一个直接内存的ByteBuf，
     */
    @Override
    protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity) {
        //先获得执行当前方法的内存池
        PoolThreadCache cache = threadCache.get();
        //获得内存池中持有的PoolArena
        PoolArena<ByteBuffer> directArena = cache.directArena;
        final ByteBuf buf;
        //if (directArena != null) {
        //从directArena中分配内存
        buf = directArena.allocate(cache, initialCapacity, maxCapacity);
        //}
        //在Netty中，默认使用的是池化的直接内存，所以通常情况下是走不到这个分枝里的，这节课我们还没有引入下面两个类，所以
        //我就直接把下面注释掉了。
//        else {
//            buf = PlatformDependent.hasUnsafe() ?
//                    UnsafeByteBufUtil.newUnsafeDirectByteBuf(this, initialCapacity, maxCapacity) :
//                    new UnpooledDirectByteBuf(this, initialCapacity, maxCapacity);
//        }
        //将ByteBuf包装一下，可以检测内存是否泄漏。这里其实就是把Buf放进另一种Buf中
        //return toLeakAwareBuffer(buf);
        return buf;
    }


    public static int defaultNumHeapArena() {
        return DEFAULT_NUM_HEAP_ARENA;
    }


    public static int defaultNumDirectArena() {
        return DEFAULT_NUM_DIRECT_ARENA;
    }


    public static int defaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }


    public static int defaultMaxOrder() {
        return DEFAULT_MAX_ORDER;
    }


    public static boolean defaultUseCacheForAllThreads() {
        return DEFAULT_USE_CACHE_FOR_ALL_THREADS;
    }


    public static boolean defaultPreferDirect() {
        return PlatformDependent.directBufferPreferred();
    }


    public static int defaultTinyCacheSize() {
        return DEFAULT_TINY_CACHE_SIZE;
    }


    public static int defaultSmallCacheSize() {
        return DEFAULT_SMALL_CACHE_SIZE;
    }


    public static int defaultNormalCacheSize() {
        return DEFAULT_NORMAL_CACHE_SIZE;
    }


    public static boolean isDirectMemoryCacheAlignmentSupported() {
        return PlatformDependent.hasUnsafe();
    }

    @Override
    public boolean isDirectBufferPooled() {
        return directArenas != null;
    }


    @Deprecated
    public boolean hasThreadLocalCache() {
        return threadCache.isSet();
    }


    @Deprecated
    public void freeThreadLocalCache() {
        threadCache.remove();
    }

    final class PoolThreadLocalCache extends FastThreadLocal<PoolThreadCache> {
        private final boolean useCacheForAllThreads;
        //useCacheForAllThreads的值为true，意味着给每个线程都创建内存池，不管该线程是否属于FastThreadLocalThread类型
        PoolThreadLocalCache(boolean useCacheForAllThreads) {
            this.useCacheForAllThreads = useCacheForAllThreads;
        }

        /**
         * @Author: ytrue
         * @Description:该方法就是用来获得内存池的方法
         */
        @Override
        protected synchronized PoolThreadCache initialValue() {
            //寻找被使用次数最少的heapArena
            final PoolArena<byte[]> heapArena = leastUsedArena(heapArenas);
            //寻找被使用次数最少的directArena
            final PoolArena<ByteBuffer> directArena = leastUsedArena(directArenas);
            //获得执行当前方法的线程
            final Thread current = Thread.currentThread();
            if (useCacheForAllThreads || current instanceof FastThreadLocalThread) {
                //创建内存池
                final PoolThreadCache cache = new PoolThreadCache(
                        heapArena, directArena, tinyCacheSize, smallCacheSize, normalCacheSize,
                        DEFAULT_MAX_CACHED_BUFFER_CAPACITY, DEFAULT_CACHE_TRIM_INTERVAL);
                //DEFAULT_CACHE_TRIM_INTERVAL_MILLIS这个值的默认值为0，所以并不会提交下面的定时任务，也就是不会
                //定期释放线程的内存池
                if (DEFAULT_CACHE_TRIM_INTERVAL_MILLIS > 0) {
                    final EventExecutor executor = ThreadExecutorMap.currentExecutor();
                    if (executor != null) {
                        //这里虽然注册了一个定时任务，定期释放内存池的内存，但是并不会真的执行
                        executor.scheduleAtFixedRate(trimTask, DEFAULT_CACHE_TRIM_INTERVAL_MILLIS,
                                DEFAULT_CACHE_TRIM_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
                    }
                }
                return cache;
            }
            return new PoolThreadCache(heapArena, directArena, 0, 0, 0, 0, 0);
        }

        @Override
        protected void onRemoval(PoolThreadCache threadCache) {
            threadCache.free(false);
        }


        /**
         * @Author: ytrue
         * @Description:寻找被使用次数最少的Arena
         */
        private <T> PoolArena<T> leastUsedArena(PoolArena<T>[] arenas) {
            if (arenas == null || arenas.length == 0) {
                return null;
            }
            PoolArena<T> minArena = arenas[0];
            for (int i = 1; i < arenas.length; i++) {
                PoolArena<T> arena = arenas[i];
                if (arena.numThreadCaches.get() < minArena.numThreadCaches.get()) {
                    minArena = arena;
                }
            }
            return minArena;
        }
    }

    @Override
    public PooledByteBufAllocatorMetric metric() {
        return metric;
    }


    @Deprecated
    public int numHeapArenas() {
        return heapArenaMetrics.size();
    }


    @Deprecated
    public int numDirectArenas() {
        return directArenaMetrics.size();
    }


    @Deprecated
    public List<PoolArenaMetric> heapArenas() {
        return heapArenaMetrics;
    }


    @Deprecated
    public List<PoolArenaMetric> directArenas() {
        return directArenaMetrics;
    }


    @Deprecated
    public int numThreadLocalCaches() {
        PoolArena<?>[] arenas = heapArenas != null ? heapArenas : directArenas;
        if (arenas == null) {
            return 0;
        }

        int total = 0;
        for (PoolArena<?> arena : arenas) {
            total += arena.numThreadCaches.get();
        }

        return total;
    }


    @Deprecated
    public int tinyCacheSize() {
        return tinyCacheSize;
    }


    @Deprecated
    public int smallCacheSize() {
        return smallCacheSize;
    }


    @Deprecated
    public int normalCacheSize() {
        return normalCacheSize;
    }


    @Deprecated
    public final int chunkSize() {
        return chunkSize;
    }

    final long usedHeapMemory() {
        return usedMemory(heapArenas);
    }

    final long usedDirectMemory() {
        return usedMemory(directArenas);
    }

    private static long usedMemory(PoolArena<?>[] arenas) {
        if (arenas == null) {
            return -1;
        }
        long used = 0;
        for (PoolArena<?> arena : arenas) {
            used += arena.numActiveBytes();
            if (used < 0) {
                return Long.MAX_VALUE;
            }
        }
        return used;
    }

    final PoolThreadCache threadCache() {
        PoolThreadCache cache =  threadCache.get();
        assert cache != null;
        return cache;
    }


    public boolean trimCurrentThreadCache() {
        PoolThreadCache cache = threadCache.getIfExists();
        if (cache != null) {
            cache.trim();
            return true;
        }
        return false;
    }


    public String dumpStats() {
        int heapArenasLen = heapArenas == null ? 0 : heapArenas.length;
        StringBuilder buf = new StringBuilder(512)
                .append(heapArenasLen)
                .append(" heap arena(s):")
                .append(StringUtil.NEWLINE);
        if (heapArenasLen > 0) {
            for (PoolArena<byte[]> a: heapArenas) {
                buf.append(a);
            }
        }

        int directArenasLen = directArenas == null ? 0 : directArenas.length;

        buf.append(directArenasLen)
                .append(" direct arena(s):")
                .append(StringUtil.NEWLINE);
        if (directArenasLen > 0) {
            for (PoolArena<ByteBuffer> a: directArenas) {
                buf.append(a);
            }
        }

        return buf.toString();
    }
}
