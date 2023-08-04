package com.ytrue.netty.util;

import com.ytrue.netty.util.concurrent.FastThreadLocal;
import com.ytrue.netty.util.internal.SystemPropertyUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ytrue.netty.util.internal.MathUtil.safeFindNextPositivePowerOfTwo;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author ytrue
 * @date 2023-08-04 9:07
 * @description 这个类就是对象池的核心类，可以这么说，和对象池有关的所有类都被定义在该类中了，作为内部类存在
 * 对象池究竟是什么？首先要清楚一个概念，对象池是对于线程而言的，每个线程都有自己的对象缓存，也就是对象池，
 * 可以这么说，对象池实际上就是每一个线程的本地栈，也就是stack。而Recycler这个类，在我看来，这个类只不过是每个线程
 * 从各自对象池中获取对象的入口。由该类的方法得到fastthreadlocal，然后得到本地线程的map，每个线程的stack就存储在这个
 * 私有的本地map当中，是真正的对象池
 */
@Slf4j
public abstract class Recycler<T> {


    /**
     * 该属性是是用来给线程提供特殊化id的，这个id是给生产对象的线程和回收对象的线程赋值的，
     * 并且就用赋的这个值区分这两种类型的线程
     */
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(Integer.MIN_VALUE);

    /**
     * 这个属性被static和final修饰，这说明该属性一旦被初始化就不会被改变，并且只初始化一次
     * 这个属性就是给所有从对象池中获得了对象的线程赋值的。这么说可能太抽象，我给大家举一个例子
     * 比如，有一个线程调用Recycler中的方法，得到了它私有的stack，由该stack获得了对象。但是该对象在该线程中用完了
     * 可能会被传递到其他线程中，最后回收的时候，由其他对象帮忙回收。
     * 而下面的这个属性就是标志着创建对象的那个线程id。如果一个线程从自己的stack中拿到了对象，那这个线程就会被下面这个属性做上标记
     * 从名称上看这个属性是个id，并且是唯一的，这说明多个线程共用这一个id。这里又要做一番解释，这里的这个id只是用来区分从自己stack中得到
     * 对象或者说是创建对象的线程和帮忙回收其他线程对象的线程。
     * 简单来说，如果一个线程帮忙其他线程回收对象，那这个帮忙其他线程回收对象的线程会单独享有一个id
     * 而所有自己从自己的stack中获得对象，最后又由自己返回对象池的线程，共享这一个id
     * 我们只用下面这个id区分正在回收对象的线程究竟回收的是自己对象还是其他线程的对象即可
     */
    private static final int OWN_THREAD_ID = ID_GENERATOR.getAndIncrement();


    /**
     * 每一个线程对应的stack可以存储的数据的最大个数，stack存储数据的是一个数组容器，默认最大容量为4096
     * 毕竟对象池不能无限扩大。
     */
    private static final int DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD = 4 * 1024;


    /**
     * 这个属性就是staack最大的容量值，上面的属性最后会赋值给它
     */
    private static final int DEFAULT_MAX_CAPACITY_PER_THREAD;


    /**
     * 这是一个辅助属性，帮助计算所有帮忙回收对象的线程能够为原来获取对象的线程能够回收的对象的最大值
     * 这个也是在限制对象池的容量，不能让它无限扩大，默认值为2。
     */
    private static final int MAX_SHARED_CAPACITY_FACTOR;


    /**
     * 每一个线程最多可以帮助几个线程回收对象。默认值是cpu的核数乘以2
     * 其实就是默认的工作组线程的数量
     * 这个数值在Netty中用得比较多，内存池的时候也会用到，那里其实就是为了让每一个线程都能参与进来
     */
    private static final int MAX_DELAYED_QUEUES_PER_THREAD;


    /**
     * link链表中每一个link节点使用数组存储待回收的对象，这个数组的默认容量为16
     * 存储满了就找下一个link节点，不会扩容
     */
    private static final int LINK_CAPACITY;


    /**
     * stack默认的初始容量，为256个
     */
    private static final int INITIAL_CAPACITY;


    /**
     * 对象被回收的比例。默认是8比1。比如一个线程从线程池也就是它的本地stack中得到了8个对象，那么就把第8个对象回收进来
     * 这里也是为了防止对象池无限膨胀。大家现在可能觉得能创建多少对象啊，至于这么限制吗？
     * 但是想一想，Netty中所有数据的处理都离不开ByteBuf这个对象。成百上千个客户端发送数据的时候要创建的ByteBuf多了去了
     * 这个对象就要从对象池中获得，如果每一个都被回收，那对象池就会把内存占满了。
     */
    private static final int RATIO;

    /**
     * handle实际上就是从对象池中获得对象，而handler中的一个成员变量是真正我们需要的对象
     */
    private static final Handle NOOP_HANDLE = new Handle() {
        /**
         * 回收利用
         * @param object
         */
        @Override
        public void recycle(Object object) {
            // NOOP
        }
    };


    //下面这些属性的赋值实际上都是可以由jvm参数调整的，用户可以自行设定这些参数的值
    static {
        //这个就是stack中数组最大容量的赋值
        int maxCapacityPerThread = SystemPropertyUtil.getInt("io.netty.recycler.maxCapacityPerThread",
                SystemPropertyUtil.getInt("io.netty.recycler.maxCapacity", DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD));

        if (maxCapacityPerThread < 0) {
            //最大容量赋值给了maxCapacityPerThread属性
            maxCapacityPerThread = DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD;
        }
        //maxCapacityPerThread又赋值给了该属性。
        DEFAULT_MAX_CAPACITY_PER_THREAD = maxCapacityPerThread;

        //这个就是我们之前说的那个辅助的属性，默认值为2，帮助计算每个线程最多可以帮助其他线程回收多少个对象
        MAX_SHARED_CAPACITY_FACTOR = max(2,
                SystemPropertyUtil.getInt("io.netty.recycler.maxSharedCapacityFactor",
                        2));

        //每一个线程最多可以帮助几个线程回收对象。默认值是cpu的核数乘以2
        MAX_DELAYED_QUEUES_PER_THREAD = max(0,
                SystemPropertyUtil.getInt("io.netty.recycler.maxDelayedQueuesPerThread",
                        // We use the same value as default EventLoop number
                        NettyRuntime.availableProcessors() * 2));

        //link链表中每一个link节点使用数组存储待回收的对象，这个数组的默认容量为16
        LINK_CAPACITY = safeFindNextPositivePowerOfTwo(
                max(SystemPropertyUtil.getInt("io.netty.recycler.linkCapacity", 16), 16));

        //对象被回收的比例。默认是8比1。
        RATIO = safeFindNextPositivePowerOfTwo(SystemPropertyUtil.getInt("io.netty.recycler.ratio", 8));

        if (log.isDebugEnabled()) {
            if (DEFAULT_MAX_CAPACITY_PER_THREAD == 0) {
                log.debug("-Dio.netty.recycler.maxCapacityPerThread: disabled");
                log.debug("-Dio.netty.recycler.maxSharedCapacityFactor: disabled");
                log.debug("-Dio.netty.recycler.linkCapacity: disabled");
                log.debug("-Dio.netty.recycler.ratio: disabled");
            } else {
                log.debug("-Dio.netty.recycler.maxCapacityPerThread: {}", DEFAULT_MAX_CAPACITY_PER_THREAD);
                log.debug("-Dio.netty.recycler.maxSharedCapacityFactor: {}", MAX_SHARED_CAPACITY_FACTOR);
                log.debug("-Dio.netty.recycler.linkCapacity: {}", LINK_CAPACITY);
                log.debug("-Dio.netty.recycler.ratio: {}", RATIO);
            }
        }

        //stack中的数组默认的初始容量，为256个，如果存储的属性不够了就要扩容
        //默认最大容量为4096，毕竟对象池不能无限扩大。
        INITIAL_CAPACITY = min(DEFAULT_MAX_CAPACITY_PER_THREAD, 256);
    }


    //线程的stack中数组的最大容量，实际上就是对象池的最大容量
    private final int maxCapacityPerThread;
    //这个属性就是帮助计算所有帮忙回收对象的线程能够为原来获取对象的线程能够回收的对象的最大值
    private final int maxSharedCapacityFactor;
    //这个就是每个线程回收自身对象的比例
    private final int ratioMask;
    //每个线程最多可以帮助几个线程回收它们的对象
    private final int maxDelayedQueuesPerThread;


    /**
     * 这里之所以用弱引用来包装key，还是考虑到stack对应的线程挂掉了，弱引用可以帮助垃圾回收
     * 对应的value在下一次操作map时也就被相应回收了
     * 这里我们先不讲解这个属性是什么，后面自会知晓
     */
    private static final FastThreadLocal<Map<Stack<?>, WeakOrderQueue>> DELAYED_RECYCLED =
            new FastThreadLocal<Map<Stack<?>, WeakOrderQueue>>() {
                @Override
                protected Map<Stack<?>, WeakOrderQueue> initialValue() {
                    return new WeakHashMap<>();
                }
            };


    /**
     * 这个就是对象池了，说是对象池，实际上就是每个线程私有的对象池的入口，也就是stack的入口
     * 通过这个FastThreadLocal可以得到每个线程私有的map，进一步得到存储在map中的stack。
     * 注意，这个属性并不是静态的，这就说明，如果在一个线程中，同时调用了两个不同的对象池的入口方法，那就会存在两个threadlocal
     * 而且每一个threadlocal都可以得到线程私有的map，这就意味着有两个stack存储在每个线程私有的map中，
     * 也就是说，一个线程可以同时拥有多个栈，只要这个线程调用了不同对象池的入口方法
     * 这时候，我们就可以成这个线程拥有了两个对象池
     * 其实这里大家也可以看出来，Netty为了提高自身的性能，采用了一种空间换时间的思想策略。不用加锁，用线程私有的属性，让每个线程
     * 去各自线程持有的对象池中获得各自的对象。虽然占用的内存多了，但是节省了很多原本需要加锁解决并发的开销
     */
    private final FastThreadLocal<Stack<T>> threadLocal = new FastThreadLocal<Stack<T>>() {
        //这个就是返回线程私有栈stack的方法，这个不用多讲了吧，这是fastthreadlocal的知识。
        @Override
        protected Stack<T> initialValue() {
            return new Stack<T>(
                    Recycler.this,
                    Thread.currentThread(),
                    maxCapacityPerThread,
                    maxSharedCapacityFactor,
                    ratioMask,
                    maxDelayedQueuesPerThread
            );
        }

        //这个方法是删除线程私有map中数据的扩展方法
        @Override
        protected void onRemoval(Stack<T> value) {
            // Let us remove the WeakOrderQueue from the WeakHashMap directly if its safe to remove some overhead
            if (value.threadRef.get() == Thread.currentThread()) {
                if (DELAYED_RECYCLED.isSet()) {
                    DELAYED_RECYCLED.get().remove(value);
                }
            }
        }
    };


    protected Recycler() {
        this(DEFAULT_MAX_CAPACITY_PER_THREAD);
    }

    protected Recycler(int maxCapacityPerThread) {
        this(maxCapacityPerThread, MAX_SHARED_CAPACITY_FACTOR);
    }

    protected Recycler(int maxCapacityPerThread, int maxSharedCapacityFactor) {
        this(maxCapacityPerThread, maxSharedCapacityFactor, RATIO, MAX_DELAYED_QUEUES_PER_THREAD);
    }

    protected Recycler(
            int maxCapacityPerThread,
            int maxSharedCapacityFactor,
            int ratio,
            int maxDelayedQueuesPerThread
    ) {
        ratioMask = safeFindNextPositivePowerOfTwo(ratio) - 1;
        //maxCapacityPerThread的值已经被赋成4096了，所以一定是大于0的，会走到下面的分支，给各个属性赋值
        if (maxCapacityPerThread <= 0) {
            this.maxCapacityPerThread = 0;
            this.maxSharedCapacityFactor = 1;
            this.maxDelayedQueuesPerThread = 0;
        } else {
            //线程的stack中数组的最大容量赋值
            this.maxCapacityPerThread = maxCapacityPerThread;
            //帮助计算每个线程可帮助原来获取对象的线程能回收的最多对象个数的辅助属性赋值
            this.maxSharedCapacityFactor = max(1, maxSharedCapacityFactor);
            //每个线程最多可以帮助几个线程回收它们的对象赋值
            this.maxDelayedQueuesPerThread = max(0, maxDelayedQueuesPerThread);
        }
    }


    /**
     * 对象池的重点方法，我称它为每个线程的对象池的入口方法，就是通过这个方法，每个线程得到了
     * 各自的stack，stack就是每个线程自己的对象池。由此可见，Recycler类其实就是对每个线程的对象池做了一个封装
     *
     * @return
     */
    public final T get() {
        //这里会判断线程的stack对象池的数组最大容量是否等于0，
        //如果等于0，就创建一个对象直接返回，而且该对象用完就用完了，不会被回收进对象池等待再次利用
        //这里大家可能还不太明白，但是再往下看就会知道，凡是可以被回收的对象，都会被一个handl包装一下。
        if (maxCapacityPerThread == 0) {
            return newObject((Handle<T>) NOOP_HANDLE);
        }
        //从线程的私有map中获得stack，这里就是通过之前创建的fastthreadlocal得到私有的map，其实map中存储数据的是数组，数组的下标
        //由fastthreadlocal确定了。接着就取出固定下标位置的stack，也就是该线程拥有的一个对象池
        Stack<T> stack = threadLocal.get();
        //从stack对象池中取出被缓存的对象，这里虽然得到的是一个handle，但实际上被缓存的对象就是handle的一个成员变量
        //该对象回收时，也是通过该handle回收的。所以我们可以说，是handle对被缓存的对象做了一层包装
        DefaultHandle<T> handle = stack.pop();

        //如果handle为null，说明对象池中还没 有缓存的对象
        if (handle == null) {
            //首先创建一个handle
            handle = stack.newHandle();
            //然后创建一个新的要被线程使用的对象交给handle
            //这里调用了newObject方法，这个方法就是Recycler类中需要重写的一个抽象方法
            //要把哪个类的对象设置成对象池模式，就在哪个类中创建Recycler对象，然后重写newObject方法。
            handle.value = newObject(handle);
        }
        ///如果handle不为null，说明对象池中有缓存的对象，直接从handle中取出对象返回即可
        return (T) handle.value;
    }


    /**
     * 通过Recycler类来回收对象的方法，已经被废弃了
     *
     * @param o
     * @param handle
     * @return
     */
    @Deprecated
    public final boolean recycle(T o, Handle<T> handle) {
        if (handle == NOOP_HANDLE) {
            return false;
        }

        DefaultHandle<T> h = (DefaultHandle<T>) handle;
        if (h.stack.parent != this) {
            return false;
        }
        //实际上仍然是调用了handle方法来回收的
        h.recycle(o);
        return true;
    }


    /**
     * 该方法是为了得到线程的对象池的容量，也就是stack中数组的容量
     * threadLocal.get()得到的就是线程私有的stack
     *
     * @return
     */
    final int threadLocalCapacity() {
        return threadLocal.get().elements.length;
    }

    /**
     * 该方法是为了得到线程的对象池stack中数组存储元素的个数
     *
     * @return
     */
    final int threadLocalSize() {
        return threadLocal.get().size;
    }


    /**
     * 之前提到的需要在池化的类中实现的抽象方法
     *
     * @param handle
     * @return
     */
    protected abstract T newObject(Handle<T> handle);

    /**
     * Recycler类中的定义的内部接口，实现也是在本类中实现的。
     *
     * @param <T>
     */
    public interface Handle<T> {
        /**
         * 这个方法就是handle用来回收对象的方法
         *
         * @param object
         */
        void recycle(T object);
    }

    /**
     * 接口的静态实现类
     *
     * @param <T>
     */
    static final class DefaultHandle<T> implements Handle<T> {

        /**
         * 回收对象线程的标记
         * 这个属性就是用来标志这个对象暂时被哪个线程回收了。这里可以看到，当一个线程从自己的对象池中获得了
         * 对象，之后这个对象却不由自己回收，而是交给另外的线程来回收，那么下面的这个属性就会用来标记暂时回收对象的那个线程
         * 这么做是为了和获得对象的线程和帮助回收对象的线程作区分
         */
        private int lastRecycledId;


        /**
         * 获取对象线程的标记
         * 这个属性就是用来标记从对象池中获得的对象最终被哪个线程回收了。这个属性其实对应的就是获得对象的那个线程
         * 如果一个对象从自己的对象池中得到了一个对象，这个对象要暂时交给其他线程来帮忙回收，在其他线程还未把那个对象转移到本线程
         * 的对象池中时，这个属性就会一直是0。
         */
        private int recycleId;

        /**
         * 判断线程是否被回收了
         */
        boolean hasBeenRecycled;

        /**
         * value 这个对象属于那个对象池
         * handle中是要持有stack的，这个很重要。因为handle要知道这个对象本身是从那个线程的对象池中获得的
         */
        private Stack<?> stack;

        /**
         * 这个属性就是要被池化的那个对象,就是我们的目标对象
         */
        private Object value;

        DefaultHandle(Stack<?> stack) {
            this.stack = stack;
        }


        @Override
        public void recycle(T object) {
            //如果对象为null，直接抛出异常
            if (object != value) {
                throw new IllegalArgumentException("object does not belong to handle");
            }

            //这里得到了对象所属的线程池，也就是那个线程的stack
            //这里是怎么得到stack的呢？
            //在上面的get方法中有这个方法 stack.newHandle();
            //stack调用它自己的方法创建一个handle时，会把自身传到handle的构造器中。所以handle就持有了创建对象的那个stack
            Stack<?> stack = this.stack;

            //虽然还没讲到WeakOrderQueue那个地方，但这里有必要给大家先讲解一下，算是混个眼熟，到下面可能就直接明白了。
            //当一个对象要被放回对象池的时候，但是此时是另一个线程在做回收的工作，此时我们就要用两个id来进行标记。看看这个对象的放回
            //工作进展到什么程度了。因为，就算是其他线程帮助某个线程在做回收对象的工作。这个对象最终还是要放回到它本来的线程对应的对象池中
            //recycleId代表的是该对象原本所属的对象池的那个线程
            //lastRecycledId代表的是帮助该对象回收的那个线程。如果对象是由其他线程帮助回收的，就要先放到其他线程的一个队列中
            //然后该对象所属的那个线程会在某个时刻把该对象从其他线程的队列中回收过来。回收完成的那一刻，recycleId和lastRecycledId就会相等了
            //当一个对象刚刚被从对象池中取出来，该对象所在的handle的recycleId和lastRecycledId是相等的，都为0。
            //只要这个对象被取走了，没有被本线程回收，那么recycleId属性就会一直是0
            //而该对象如果要被其他线程帮忙回收，暂时放在其他线程的WeakOrderQueue队列中了，这时候帮忙回收的线程就会被lastRecycledId值标记
            //lastRecycledId属性的值会和recycleId不一样了。直到该对象被原本的线程回收，这两个值会再次相等。
            //理解了这个我们再来看下面这行代码，为什么经过lastRecycledId != recycleId 判断之后，如果为true就报错？
            //因为这个lastRecycledId不等于recycleId了，而该方法又是在recycle回收方法之内，这时候对象还未被回收，recycleId肯定为0
            //而lastRecycledId不为0了，说明该对象已经被其他线程帮忙回收了，放在其他线程的队列中的，但是又在recycle方法内回收它，
            //重复回收所以就报错了
            if (lastRecycledId != recycleId || stack == null) {
                throw new IllegalStateException("recycled already");
            }
            //把对象回收到线程对应的stack对象池中
            stack.push(this);
        }
    }


    /**
     * 对象池啦
     *
     * @param <T>
     */
    static final class Stack<T> {

        /**
         * stack持有本类的引用，也就是Recycler对象
         */
        final Recycler<T> parent;

        /**
         * 这个threadRef我们之前分析过，这个就是stack对象池对应的所属线程，但这里为什么要采用弱引用的方式呢？
         * 我们思考这样一种情况，现在，stack引用的所属线程的线程，而handl又持有了其对应的stack，所以handle可以得到stack
         * 所以handle得到stack，stack又可以得到线程，如果对象池对应的线程挂掉了，可是stack中还持有这个线程的强引用，
         * 就不利于线程的垃圾回收，所以这里搞成弱引用，帮助垃圾回收而已
         */
        final WeakReference<Thread> threadRef;

        /**
         * 能够帮助从对象池获得对象的那个线程回收的对象最大值
         * 其实就是用于WeakOrderQueue创建Link个数的限制
         */
        final AtomicInteger availableSharedCapacity;

        /**
         * 当前线程可以最多帮助几个线程回收对象
         */
        final int maxDelayedQueues;

        /**
         * stack中数组的最大容量
         */
        private final int maxCapacity;

        /**
         * 回收对象的比例
         */
        private final int ratioMask;

        /**
         * stack中存储对象的数组容器
         */
        private DefaultHandle<?>[] elements;

        /**
         * 当前stack数组中存储了多少个对象
         */
        private int size;

        /**
         * 这个属性是和回收对象比例有关的，要配合ratioMask一起使用的
         */
        private int handleRecycleCount = -1;

        /**
         * 终于看到WeakOrderQueue节点了，stack中既有数组，同时也存在一个WeakOrderQueue对象构成的链表
         * 定义了当前节点和前驱节点
         */
        private WeakOrderQueue cursor;
        private WeakOrderQueue prev;


        /**
         * WeakOrderQueue头节点
         */
        private volatile WeakOrderQueue head;


        /**
         * 构造
         *
         * @param parent                  stack持有本类的引用，也就是Recycler对象
         * @param thread                  这个就是stack对象池对应的所属线程
         * @param maxCapacity             stack中数组的最大容量
         * @param maxSharedCapacityFactor
         * @param ratioMask               回收对象的比例
         * @param maxDelayedQueues        当前线程可以最多帮助几个线程回收对象
         */
        Stack(
                Recycler<T> parent,
                Thread thread,
                int maxCapacity,
                int maxSharedCapacityFactor,
                int ratioMask,
                int maxDelayedQueues
        ) {
            this.parent = parent;
            threadRef = new WeakReference<Thread>(thread);
            this.maxCapacity = maxCapacity;
            availableSharedCapacity = new AtomicInteger(max(maxCapacity / maxSharedCapacityFactor, LINK_CAPACITY));
            elements = new DefaultHandle[min(INITIAL_CAPACITY, maxCapacity)];
            this.ratioMask = ratioMask;
            this.maxDelayedQueues = maxDelayedQueues;
        }

        /**
         * 之前解释过，插入WeakOrderQueue头节点的时候是头插法，并且是多个线程一起工作的，所以会存在并发问题
         * 所以在这里用了对象池中唯一的一个锁
         *
         * @param queue
         */
        synchronized void setHead(WeakOrderQueue queue) {
            // queue.next = head
            queue.setNext(head);
            // 再把头部从新设置一下
            head = queue;
        }


        /**
         * 对stack的数组扩容
         *
         * @param expectedCapacity expectedCapacity是需要的容量，而扩容之后的容量就是所需容量的最接近的2次幂
         * @return
         */
        int increaseCapacity(int expectedCapacity) {
            int newCapacity = elements.length;
            int maxCapacity = this.maxCapacity;
            do {
                newCapacity <<= 1;
            } while (newCapacity < expectedCapacity && newCapacity < maxCapacity);
            //expectedCapacity是需要的容量，而扩容之后的容量就是所需容量的最接近的2次幂
            newCapacity = min(newCapacity, maxCapacity);
            if (newCapacity != elements.length) {
                //把数据拷贝到新的数组
                elements = Arrays.copyOf(elements, newCapacity);
            }
            return newCapacity;
        }


        /**
         * 这个就是从stack对象池获取缓存对象的方法，
         * 注意，每次从对象池中获取缓存对象的时候，就会把其他线程帮忙回收的对象从其他线程的WeakOrderQueue对象中移动到
         * 自己的stack中，这个方法是被stack调用的，也就是原本从对象池中获取对象的那个线程调用的，这个要搞清楚
         *
         * @return
         */
        DefaultHandle<T> pop() {
            //得到数组中存储的对象的个数
            int size = this.size;

            if (size == 0) {
                // 如果为0，说明当前stack中没有可以取出的对象，这时候就要从其他帮忙回收对象的线程的WeakOrderQueue对象中
                // 看看是否有可以转移回来的对象，如果有就转移回来，如果也没有，就直接返回null。
                if (!scavenge()) {
                    //WeakOrderQueue对象中没有数据就直接返回null
                    return null;
                }
                //走到这里说明WeakOrderQueue对象中有数据，转移完成之后重新给size赋值
                size = this.size;
            }

            //要从stack中拿走一个对象，所以要减1
            size--;
            //存储对象的个数减1正好得到数组存储的最后一个对象的数组下标，取出该下标的对象
            DefaultHandle ret = elements[size];
            //把数组原位置置为null
            elements[size] = null;
            //这里又遇到这种判断了。这里其实应该是相等的，下面的方法会有具体逻辑，如果不想等就报错
            if (ret.lastRecycledId != ret.recycleId) {
                throw new IllegalStateException("recycled multiple times");
            }
            //刚从对象池拿出来的时候这两个属性都设置为0
            ret.recycleId = 0;
            ret.lastRecycledId = 0;
            //取走了一个对象，要重新给size赋值
            this.size = size;
            //返回该handle对象
            return ret;
        }


        /**
         * 该方法就是扫描其他线程的WeakOrderQueue对象中是否有对象
         *
         * @return
         */
        boolean scavenge() {
            //这一步就是从其他线程的WeakOrderQueue对象中把属于自己对象池的对象移回到自己的stack中
            if (scavengeSome()) {
                return true;
            }
            //如果所有WeakOrderQueue节点中都没有对象，就把当前节点和前驱节点重新初始化，因为在查找的过程中，这两个值已经发生
            //改变了
            prev = null;
            cursor = head;
            return false;
        }


        /**
         * 这个可以说是stack类中最重要的一个方法了，这个方法的作用就是检查WeakOrderQueue链表中的节点,如果有数据就回收到自己的stack中
         *
         * @return
         */
        boolean scavengeSome() {
            //先定义好前驱节点
            WeakOrderQueue prev;

            //也定义好当前节点，这时候当前节点还是null
            WeakOrderQueue cursor = this.cursor;

            //这里会有一个判断，看当前节点是否为null，如果为null，说明是第一次扫描WeakOrderQueue链表
            if (cursor == null) {
                prev = null;
                //把当前节点设置为头节点
                cursor = head;

                if (cursor == null) {
                    //如果设置为头节点后仍然为null，说明WeakOrderQueue链表中根本就没有头节点，也就意味着还没有其他线程帮助
                    //回收对象呢，所以直接返回false即可
                    return false;
                }
            } else {
                //走到这里那当前节点肯定就不为null，也就意味着WeakOrderQueue链表中确实有数据，这时候就把前驱节点赋上值
                prev = this.prev;
            }

            //设置一个可以退出do while循环的标记
            boolean success = false;
            do {
                //在循环中遍历WeakOrderQueue链表
                //这个方法之前讲过了吧，可以看之前的逻辑，就是把WeakOrderQueue对象中的
                //数据转移到当前stack中，一次限定转移一个link中数组的容量，也就是16
                if (cursor.transfer(this)) {
                    //转移成功之后把标记设置为true
                    success = true;
                    break;
                }
                //走到这里意味着刚才的那个当前节点中没有可被回收的对象，这时候就找下一个WeakOrderQueue节点查看
                //所以先得到当前节点的下一个节点
                WeakOrderQueue next = cursor.next;
                //这里要判断一下当前节点所对应的线程是不是已经挂掉了
                //注意，这里当前节点对应的是帮助回收对象的线程
                if (cursor.owner.get() == null) {
                    //如果真的挂掉了，就判断当前节点中是否还有未被转移到stack的数据
                    if (cursor.hasFinalData()) {
                        //如果有，就把所有数据都回收一下，这里是循环回收，并不是只回收一次
                        for (; ; ) {
                            if (cursor.transfer(this)) {
                                success = true;
                            } else {
                                break;
                            }
                        }
                    }
                    //这里做了一个判断，如果前驱节点不为null，那么当前节点一定不是头节点，所以可以直接让前驱节点指向当前节点的
                    //下一个节点，把当前节点删除掉，因为对应的线程已经挂了。但是，如果前驱节点为null，那就意味着当前节点是头节点
                    //但是头节点是不会被删除的。还记得插入头节点的时候加锁了吗？这意味着头节点的变动会产生并发问题，这里既然不加锁，
                    //就不能直接删除头节点。那头节点什么时候删除？就是等待又有线程帮助该线程回收对象了，那个帮助回收对象的线程会将自己的
                    //WeakOrderQueue对象添加到WeakOrderQueue链表中成为头节点，到时候再次循环遍历WeakOrderQueue链表的时候，如果遍历到这个
                    //空的WeakOrderQueue对象时，就可以直接把它删掉了，因为这时候它已经不是头节点了
                    if (prev != null) {
                        //这里是直接操作指针删除当前节点
                        prev.setNext(next);
                    }
                } else {
                    //把当前节点赋值给前一个节点
                    prev = cursor;
                }
                //把下一个节点赋值给当前节点，方便继续遍历
                cursor = next;
                //这里是退出条件知道当前节点为null和退出标志为true
            } while (cursor != null && !success);
            //转移完了之后，给改变了的当前节点和前驱节点赋值
            this.prev = prev;
            this.cursor = cursor;
            return success;
        }


        /**
         * 该方法就是把对象放回stack中去，这里面会根据回收对象的线程做一个判断
         * 如果是从自己对象池中拿到对象的线程，就直接放回对象池中，如果是其他线程帮忙回收的，就放进其他线程的WeakOrderQueue对象中
         *
         * @param item
         */
        void push(DefaultHandle<?> item) {
            //得到执行当前方法的线程
            Thread currentThread = Thread.currentThread();
            //判断该线程是否和stack对应的线程相等
            if (threadRef.get() == currentThread) {
                //相等则表明是得到对象的线程自己回收的，那就直接放进stack中去即可
                pushNow(item);
            } else {
                //不想等则表明是其他线程帮忙回收的，那就放进其他线程对应的WeakOrderQueue对象中即可
                //注意，这里既然走到这里，就说明执行当前方法的线程已经是帮忙回收对象的线程了
                //这个currentThread参数也就是帮忙回收对象的线程。所以才会有了后来每一个帮忙回收对象的线程都有一个WeakOrderQueue对象
                pushLater(item, currentThread);
            }
        }


        /**
         * 把对象放回自己的stack中
         *
         * @param item
         */
        private void pushNow(DefaultHandle<?> item) {
            //如果是线程回收自己对象池中的对象，那么handle中的这两个属性这是还应该都还是0，是相等的。
            //其中有一个不是0，就说明已经被回收了，或者至少被放在WeakOrderQueue中了
            if ((item.recycleId | item.lastRecycledId) != 0) {
                throw new IllegalStateException("recycled already");
            }

            //自己回收自己的对象的时候，handle中的这两个属性都会被赋值为OWN_THREAD_ID
            item.recycleId = item.lastRecycledId = OWN_THREAD_ID;
            int size = this.size;
            //在这里判断一下stack存储的对象的个数是否超过了最大容量，同时也检查一下回收的频率
            if (size >= maxCapacity || dropHandle(item)) {
                //有一个不满足就不回收对象了
                return;
            }
            //走到这里说明stack允许存放的对象的个数还未到最大值，但是数组容量不够了，就把数组扩容
            if (size == elements.length) {
                //扩容至两倍
                elements = Arrays.copyOf(elements, min(size << 1, maxCapacity));
            }
            //对象真正被回收到stack中了
            elements[size] = item;
            //增添了一个对象，所以size加1
            this.size = size + 1;
        }


        /**
         * 把对象放到WeakOrderQueue对象中的方法，这个方法同样非常重要
         *
         * @param item
         * @param thread
         */
        private void pushLater(DefaultHandle<?> item, Thread thread) {
            //注意这里的视角切换，当前线程为帮助线程回收对象的线程。这个一定要理清楚
            //DELAYED_RECYCLED就是一个fastthreadlocal，每个帮助其他线程回收对象的线程会通过这个
            //得到自己拥有的map，然后从map中得到一个WeakHashMap，这个逻辑也要理清楚
            //而这个WeakHashMap里面的key就是该线程帮忙回收对象线程所拥有的stack，value就是该线程
            //自己的WeakOrderQueue
            //要记住啊，这个WeakHashMap其实就是存储在该线程私有的threadlocal的map中的
            // 获取当前线程的 map
            Map<Stack<?>, WeakOrderQueue> delayedRecycled = DELAYED_RECYCLED.get();

            //注意啊，这里的这个this，就是原stack所属的那个线程的stack，因为pushLater这个方法是在stack
            //中被调用的，可以自己去看看调用链，这个方法是由 stack.push(this)方法一路调用至此的
            //这里就是想得到WeakHashMap中stack对应的value
            WeakOrderQueue queue = delayedRecycled.get(this);

            if (queue == null) {
                //如果为空，就说明帮助其他线程回收对象的本线程是第一次帮助其他对象回收对象，所以要创建一个WeakOrderQueue对象
                //但是在创建之气要先判断这个线程能帮助回收的线程数是否超过最大值了，超过了就不能帮助其他线程回收对象了
                //delayedRecycled.size()表示当前线程已经帮助多少个线程回收对象
                //map的size就表示这个线程已经帮助几个线程回收对象了。因为一个size对应着一堆key-value
                //而key就是stack，就是帮助回收对象线程的线程池。
                if (delayedRecycled.size() >= maxDelayedQueues) {
                    //还记得这个WeakOrderQueue.DUMMY属性吧，如果WeakOrderQueue链表中的一个对象是这个，就不会帮助回收对象
                    delayedRecycled.put(this, WeakOrderQueue.DUMMY);
                    return;
                }
                //this仍然是原线程的stack，thread就是帮助回收对象的线程，一切就都连起来了
                //这里就真正为帮助回收的线程创建了一个WeakOrderQueue，然后加入到stack中WeakOrderQueue链表的头节点处
                if ((queue = WeakOrderQueue.allocate(this, thread)) == null) {
                    //创建失败就直接返回
                    return;
                }
                //创建好的key-value放进map当中
                delayedRecycled.put(this, queue);

            } else if (queue == WeakOrderQueue.DUMMY) {
                //走到这里就意味着queue不为null，但是为WeakOrderQueue.DUMMY
                //这也表示该WeakOrderQueue.DUMMY不会帮助回收对象
                return;
            }
            //这里是吧handl这个要被回收的对象，用尾插法，添加到WeakOrderQueue对象中link链表的尾节点中了
            queue.add(item);
        }


        /**
         * 该方法只有返回false，才能使对象被真正回收，回收比例确认
         *
         * @param handle
         * @return
         */
        boolean dropHandle(DefaultHandle<?> handle) {
            //先判断该对象是否已被回收
            if (!handle.hasBeenRecycled) {
                //回收计数handleRecycleCount的默认初始值为-1，++之后就成为0了，下面是先++再取值的
                //0和任意一个数做&都等于0呀，所以一定不会返回true，所以第一次回收对象时，该对象一定可以被回收
                //经过计算之后，ratioMask的值为7，所以下一行代码的意思是，只要不是8的倍数，和ratioMask做位运算
                //得到的值一定不等于0，所以返回true每则不能被回收。而每一个8的倍数就可以回收一次
                if ((++handleRecycleCount & ratioMask) != 0) {
                    return true;
                }
                //在这里设置对象已被回收
                handle.hasBeenRecycled = true;
            }
            return false;
        }

        /**
         * 创建一个handle，用来包装要被缓存的对象
         *
         * @return
         */
        DefaultHandle<T> newHandle() {
            return new DefaultHandle<T>(this);
        }
    }


    /**
     * WeakOrderQueue队列是一个重要的容器。它的由来其实很简单。
     * 有一个线程如果从它的某个对象池中取走了多个对象，然后这些对象都被其他线程使用了，由很多其他的线程帮忙回收。多个线程把多个对象
     * 放进一个对象池中，在这种情况下，实际上很容易就会发生并发问题。所以呢，Netty就在每个线程的对象池stack结构中增加了一个WeakOrderQueue
     * 属性。该成员变量本身是一个队列，队列由每个link节点组成，link节点存储的就是帮助其他线程回收的对象。
     * 由此可见，其实每个线程都会有一个WeakOrderQueue，因为每个线程都有可能帮助其他线程回收对象。
     * 这样一来，自身线程的WeakOrderQueue对象和其他线程所拥有的WeakOrderQueue对象，就构成了一个WeakOrderQueue队列
     * 当自身的线程要从对象池中获取对象时，如果没有对象，会去这个WeakOrderQueue队列中查看其他线程有没有存放着它的对象，如果有就转移到
     * 自己的stack中。这样，仍然是每一个线程自己去找自己的对象。就避免了并发问题。当然，我们说的很简单，具体逻辑还要到代码中学习。
     * 这个类上面的这个DELAYED_RECYCLED成员变量，就是用来创建每个线程的WeakOrderQueue的。
     * 并且一个WeakOrderQueue对象会对应一个回收线程id，WeakOrderQueue链表采用的还是头插法，
     * 每一个新的节点都会放到链表头部，当成头节点。
     * 以前这个类是继承了WeakReference<Thread>类，现在是直接在构造器里创建了一个WeakReference<Thread>对象
     */
    private static final class WeakOrderQueue {

        /**
         * 下面这个属性是一个标识，如果该WeakOrderQueue对应的线程帮助
         * 其他线程回收的对象超过了设定的个数，那就把这个成员标量赋值在WeakOrderQueue链表的某个节点中，
         * 标志着这个WeakOrderQueue对象不会再帮助其他线程回收对象了
         */
        static final WeakOrderQueue DUMMY = new WeakOrderQueue();


        /**
         * WeakOrderQueue对象中的head节点，头节点中指向了下一个link节点。链表从head节点内部开始了。
         * 这个head节点中的link指针指向的下一个link节点，就是转移对象时最先被查找的节点。
         * 用一句话概括就是，link节点的插入采用的是尾插法，而转移link节点数组中存储的对象，采用的是头查法，从头节点开始查找
         * 用这种方式避开了并发的情况
         */
        private final Head head;

        /**
         * WeakOrderQueue对象中link链表的尾节点，这玩意就是 head里面的link属性
         */
        private Link tail;

        /**
         * 每一个WeakOrderQueue对象中都会有下一个WeakOrderQueue节点的指针。这个链表实际上是从stack对象中延伸出来的。
         * 因为WeakOrderQueue对象就是stack类的一个成员变量，就相当于stack的头节点，头节点正好指向下一个WeakOrderQueue节点
         */
        private WeakOrderQueue next;


        /**
         * 这里可以看到，WeakOrderQueue对象中持有了一个线程，并且使用用弱引用包装的。
         * 这里的这个线程并不是原本获取对象池中对象的线程，而是帮助原本线程回收对象的线程
         */
        private final WeakReference<Thread> owner;


        /**
         * 帮助其他线程回收对象的线程的id，每一个WeakOrderQueue都对应着一个帮助回收对象的线程，而每一个这样的线程都对应着一个id
         * 但是大家也应该要注意，一个线程既可以是从对象池中获得对象的线程，也可以是帮助回收对象的线程。都是相对的。
         */
        private final int id = ID_GENERATOR.getAndIncrement();


        /**
         * 构造方法
         */
        private WeakOrderQueue() {
            owner = null;
            head = new Head(null);
        }


        /**
         * 另一个构造方法
         *
         * @param stack
         * @param thread
         */
        private WeakOrderQueue(Stack<?> stack, Thread thread) {
            //link链表的尾节点被创建
            tail = new Link();
            //创建头节点
            head = new Head(stack.availableSharedCapacity);
            //链表中还没有数据，所以暂时让头节点指向尾节点
            head.link = tail;
            //线程被包装在弱引用当中了
            owner = new WeakReference<Thread>(thread);
        }


        /**
         * WeakOrderQueue的构造方法是private修饰的，所以有一个静态方法来对外创建对象
         *
         * @param stack
         * @param thread
         * @return
         */
        static WeakOrderQueue newQueue(Stack<?> stack, Thread thread) {
            //创建一个WeakOrderQueue对象
            final WeakOrderQueue queue = new WeakOrderQueue(stack, thread);
            //还记得之前说的吗？WeakOrderQueue都是头插法插入到WeakOrderQueue链表中的
            //点进去会发现setHead方法是整个类中唯一一个加了锁了方法。因为把WeakOrderQueue对象添加到WeakOrderQueue链表头部
            //很可能是多个线程同时在进行，因为一个线程从对象池中获得的多个对象可能需要多个线程帮其回收，每个帮助回收的
            //线程都对应着一个WeakOrderQueue对象，自然会出现同时把WeakOrderQueue对象添加到头节点的情况
            //这时候就必须用synchronized来防止并发问题的出现
            //注意，这里是stack在调用setHead方法
            stack.setHead(queue);
            return queue;
        }


        /**
         * 设置WeakOrderQueue下一个节点
         *
         * @param next
         */
        private void setNext(WeakOrderQueue next) {
            assert next != this;
            this.next = next;
        }


        /**
         * 分配一个新的WeakOrderQueue对象的方法
         *
         * @param stack
         * @param thread
         * @return
         */
        static WeakOrderQueue allocate(Stack<?> stack, Thread thread) {
            // 首先判断是否还有剩余的可帮助回收的对象的数量，如果数量不够则不创建WeakOrderQueue对象
            return Head.reserveSpace(stack.availableSharedCapacity, LINK_CAPACITY) ? newQueue(stack, thread) : null;
        }


        /**
         * 添加新的未被回收的对象时，从尾节点添加，取走未被回收的对象时，从头节点取走。
         *
         * @param handle
         */
        void add(DefaultHandle<?> handle) {
            //这里看到lastRecycledId再次登场了，其实就可以把handle看成要被回收的对象，现在该对象内的lastRecycledId被id赋值了
            //而id就是WeakOrderQueue中的对应的帮助回收的线程的id，这就把我们之前讲的那个给对应上了
            //现在要被回收的对象中的lastRecycledId是id这个值，这就意味着该对象的回收进度已经进行到一半了，它还未完全被回收，
            //只是放在了帮助它回收的线程中，等它完全被回收的时候，handle中另一个属性recycleId也会被再次赋值，和lastRecycledId相等了
            handle.lastRecycledId = id;

            //找到link链表的尾节点。就像我们上面说的，从尾节点添加被回收对象，从头节点取走
            Link tail = this.tail;

            //这个writeIndex其实就可以代表是写指针，表示我们link对象中的数组写到第几位了
            int writeIndex;

            //如果当前尾部link节点容量已满，就需要创建新的link节点
            //tail.get()得到的就是writeIndex的值，为什么可以这么用，因为tail本身就是个原子类，所以自然可以调用get方法得到自身的值
            //如果是第一次来找要被回收的对象，可能里面还没有数据，所以现在得到的是0
            //下面这行代码的意思是，如果写指针的值为16，就说明现在这个link节点已经满了。要创建新的link节点，从链表尾部添加进去
            if ((writeIndex = tail.get()) == LINK_CAPACITY) {
                //判断可帮助创建的剩余对象数量还够不够，不够就直接返回
                if (!head.reserveSpace(LINK_CAPACITY)) {
                    return;
                }
                //创建一个新的Link节点
                //这段代码就是在重置link链表的尾节点。新创建的link对象就成了新的尾节点
                this.tail = tail = tail.next = new Link();
                //新的尾节点还未添加数据，所以写指针当然是0
                writeIndex = tail.get();
            }

            //走到这里尾节点一定是有空间的了，link对象中的数组一定有容量
            //所以就把要回收的对象handle放到数组的可写位置
            tail.elements[writeIndex] = handle;

            //这里有个很有意思的地方，就是可以看到handle中的stack会被置为null，我们知道handle中的stack实际上对应的就是原来线程
            //的对象池，而这个stack对象内部其实持有者它对应的线程。所以我们可以通过stack这个属性找到对象池所属的线程。
            //但这里把该属性置为null了。这是考虑到在对象被回收的过程中如果原来对象池对应的线程突然挂掉了，线程挂掉了那对应的
            //stack对象池也就不能再被使用了。所以这里把stack设置为null，是为了消除这个强引用，帮助jvm垃圾回收挂掉的线程对应的stack
            //但是如果线程没挂掉呢？这里却置为null了，这说明一定有一个地方还会把handle中的stack还原。没错，就是在该对象被转移回自己的stack时还原了
            handle.stack = null;

            // we lazy set to ensure that setting stack to null appears before we unnull it in the owning thread;
            // this also means we guarantee visibility of an element in the queue if we see the index updated
            //上面这两句是源码中的英文注释，我大概讲解一下，首先tail是link类型的对象，而link又继承了原子类，
            //所以tail中的value属性，也就是原子类中俄private volatile int value属性，是被volatile修饰的
            //这里用lazySet懒设置writeIndex的新值，放进去一个数据，写指针就要加1呀，其实是为了不执行内存屏障的指令。
            //我们都知道volatile可以防止指令重排，还可以给我们的代码施加读写屏障，保证内存可见性。但我们这里实际上没必要保证可见行
            //在WeakOrderQueue对象中的link链表中添加对象不会有并发问题，因为一个WeakOrderQueue对应着一个帮助回收的线程，
            //这个add方法是在帮助回收的线程中进行的，而被回收的对象也是添加到帮助回收的线程的WeakOrderQueue对象中
            //每个线程和每个线程都是独立的，所以就算有很多个线程一起回收对象，但是添加对象到WeakOrderQueue的过程是没有并发问题的
            //因为每个帮助回收的线程都有自己的WeakOrderQueue对象。虽然从link节点中取走等待被回收的对象和添加对象到link节点中是不同的线程
            //进行的，但是没必要保证立即可见行，因为这个取的时候这个对象还处于不可见的状态，那下次来的时候再取走就行了，如果没有
            //得到对象，就让愿线程重新创建一个，或者去后面的WeakOrderQueue节点中的link中继续查找。反正都是原线程
            //的对象，等待愿线程取走即可。硬要说提高了什么性能，其实也就是在把对象添加到WeakOrderQueue的link节点中时，添加的快一点而已
            //但仔细想想，追求这样的性能究竟有用吗？或者说这种做法对性能真的有提升吗？如果只是微乎其微的提升，又有什么必要呢？
            //我有时候觉得Netty中提升性能的方式甚至达到了一种吹毛求疵的效果。像是为了提升而提升，不知道大家是怎么想的。
            //反正工作中应该用不到这种提升性能的手段，学习这个就当是拓宽视野吧。
            tail.lazySet(writeIndex + 1);
        }


        /**
         * 判断WeakOrderQueue对象中是否还有可被回收的对象。
         * 这里还是再次强调一下，回收的时候都是从WeakOrderQueue的头节点link中回收的
         * 所以这里会直接用尾节点的读指针和写指针来判断。读写指针不相等，就意味着还有未被回收的对象。
         * 如果尾节点中都没有数据了，说明该WeakOrderQueue对象中一定没有数据了
         *
         * @return
         */
        boolean hasFinalData() {
            return tail.readIndex != tail.get();
        }


        /**
         * 这个可以说是WeakOrderQueue类中最重要的一个方法了，这个方法就是把要被回收的对象从WeakOrderQueue节点中
         * 移动到原来线程的stack中，该方法是在stack类中的方法内被调用的
         *
         * @param dst
         * @return
         */
        @SuppressWarnings("rawtypes")
        boolean transfer(Stack<?> dst) {
            //要从WeakOrderQueue的link节点中转移数据了
            //从头节点开始转移，所以要先获得头节点
            Link head = this.head.link;
            if (head == null) {
                //为null说明还没有要转移的数据，就直接返回false
                return false;
            }
            //这里是读指针等于16了，意思是读取到的对象已经有16个，正好是一个link数组的容量
            //这就意味着一个link节点转移完成了
            if (head.readIndex == LINK_CAPACITY) {
                //看看头节点之后是否还有下一个节点
                if (head.next == null) {
                    //没有则说明转移完了
                    return false;
                }
                //如果有就把下一个节点赋给头节点，总之永远从头节点中获取对象
                this.head.link = head = head.next;
                //这里是释放了之前的link节点，那么可以帮助一个线程回收对象的个数又可以增加16了
                this.head.reclaimSpace(LINK_CAPACITY);
            }

            //注意啊，上面的那两段代码只是一些辅助工作，真正转移数据的工作还没开始，到这里才算开始
            //得到头节点的读指针数据，知道头节点里面有多少对象已经被转移了
            final int srcStart = head.readIndex;

            //head.get()得到的是头节点的写指针，意思是真正存储了多少个对象，这个也可以看作link对象中数组的下标
            int srcEnd = head.get();

            //用读指针减去写指针，就是真正可以转移的对象个数
            final int srcSize = srcEnd - srcStart;
            //再次判断是否为0
            if (srcSize == 0) {
                return false;
            }

            //这里是获得了原来从对象池中获得了对象的那个线程的对象池中存储了多少个数据的个数
            //dst其实就是该方法传进来的stack那个参数，就是原来获得对象的线程所拥有的对象池
            final int dstSize = dst.size;

            //stack本来拥有的数据个数加上要从WeakOrderQueue对象的link节点中要转移的个数
            final int expectedCapacity = dstSize + srcSize;

            //判断这个个数是不是大于stack的长度，也就是stack数组的容量
            if (expectedCapacity > dst.elements.length) {
                //如果stack的容量不足就开始扩容
                final int actualCapacity = dst.increaseCapacity(expectedCapacity);

                //dstSize是stack之前存储了多少个对象
                //actualCapacity是扩容后stack的容量，减去dstSize是这次扩容之后还能放置多少个对象，再加上该link节点中
                //已经转移走的srcStart个对象，是不是就代表着可能从该link节点中一共转移走的对象个数的总和？
                //这里是在给srcEnd赋值，srcEnd是link中存储的元素个数。这个个数要么就是之前从原子类中取出的个数
                //要么就是从link中转移了的元素个数加上之前转移了的对象的个数
                //但是Netty中规定了从link中一次转移走的对象的个数，不能超过link的最大容量，也就是16。所以要保证
                // srcEnd，也就是写指针，也就是link中存储了多少对象的个数，在这里代表最终可以转移走的对象的数量，不能超过16
                //  已经转移3个  + 扩容后stack的容量 -  原本10个，Link存储的个数
                srcEnd = min(srcStart + actualCapacity - dstSize, srcEnd);
            }

            //终于开始转移元素了，判断读指针不等于写指针
            if (srcStart != srcEnd) {

                //得到link节点中存储对象的数组
                final DefaultHandle[] srcElems = head.elements;
                //得到stack中存储对象的数组
                final DefaultHandle[] dstElems = dst.elements;

                //不管扩没扩容，都是要先得到stack原本存储了多少数据，实际上这个值也就是stack的数组中可以存放下一个对象的
                //数组下标，这里就把回收进来的元素放在该下标上面
                int newDstSize = dstSize;

                for (int i = srcStart; i < srcEnd; i++) {

                    DefaultHandle element = srcElems[i];

                    //从link节点中取出handle之后，先判断handle的recycleId的值，如果为0，说明该对象确实还未被真正回收到stack中
                    if (element.recycleId == 0) {
                        //因为要被回收的stack中了，所以在这里就让recycleId的值等于lastRecycledId的值
                        element.recycleId = element.lastRecycledId;

                    } else if (element.recycleId != element.lastRecycledId) {
                        //走到这里就意味着recycleId不等于0，recycleId不等于0就意味着handle肯定被回收了
                        //所以直接报错
                        throw new IllegalStateException("recycled already");
                    }

                    //转移一个对象就把link节点对应的位置设置为null
                    srcElems[i] = null;

                    //还记得ratioMask这个属性吗？对象被回收的比例，每八个对象就回收第八个
                    //在这里回收到stack时也要进行该比例的控制
                    //如果这里返回true，就直接continue了，处理下一个对象，而本次回收的对象就被丢弃了，不做回收
                    //所以这里返回false才能使对象被回收
                    if (dst.dropHandle(element)) {
                        continue;
                    }

                    //还记得我们之前说的在把handl放到线程的WeakOrderQueue对象中时handle.stack = null
                    //在这里我们就要重新把handle的stack还原。
                    element.stack = dst;

                    //handle终于存放大原本它自己的stack中了
                    dstElems[newDstSize++] = element;
                }

                //srcEnd如果等于16了，意思就是这个link节点的对象都转移完了，这个是和上面的代码有关联的，并且头节点有下一个节点
                if (srcEnd == LINK_CAPACITY && head.next != null) {
                    //这里就可以增加可以帮助回收对象的数量，然后把下一个link节点设置成头节点
                    //并且帮助垃圾回收
                    this.head.reclaimSpace(LINK_CAPACITY);
                    this.head.link = head.next;
                }

                //把写指针的值赋给读指针，说明link节点的数据全都读取完了
                head.readIndex = srcEnd;

                //这里是判断，经过转移后，创建对象的线程的对象池容量有没有变化，是不是还是旧的容量，也就是存储的元素个数没变
                if (dst.size == newDstSize) {
                    return false;
                }
                //有变化就给size重新赋值
                dst.size = newDstSize;
                return true;
            } else {
                // The destination stack is full already.
                return false;
            }
        }

        static final class Link extends AtomicInteger {
            //link中用来存储待回收对象的容器，是个数组，该数组的容量为16，是默认的，如果数组满了，就会创建新的link对象，添加到link
            //链表的尾部，这里是尾插法。而WeakOrderQueue对象插入到WeakOrderQueue队列中时是头插法，要区分开
            private final DefaultHandle<?>[] elements = new DefaultHandle[LINK_CAPACITY];
            //这个相当于读指针
            private int readIndex;
            //链表指针，指向下一个link节点
            Link next;
        }

        /**
         * WeakOrderQueue对象内部的link头节点。设置这个头节点的目的很简单，
         * 我们都知道每个线程能够帮助其他线程回收的对象个数是有限的。这个限制功能就设置在了头节点之中。
         * 每次向线程对应的WeakOrderQueue对象中添加或者移走一些对象，都会经过下面这个头节点内方法的判断
         * 看看该WeakOrderQueue对象对应的线程是否帮助愿线程回收的对象达到了上限
         */
        static final class Head {

            /**
             * 这个属性就是所有帮忙回收对象的线程能够为该WeakOrderQueue对应的线程回收对象的最大个数
             * 简单来说，就是一个线程有好多个对象要其他线程帮忙回收，那其他帮忙回收的线程可以为该线程回收
             * availableSharedCapacity这么多个对象。这个值是个AtomicInteger对象，是stack中的成员变量，
             * 在不同的类中传递，但被final修饰，引用不可变，所以它的值被改变了，引用它的对象的值都会被改变
             * 所以在下面大家会看到它是用cas的方法改变自己的值的，否则会出现并发问题
             * 再强调一遍啊，这个原子对象会在多个WeakOrderQueue对象的head中被使用
             */
            private final AtomicInteger availableSharedCapacity;


            /**
             * link链表的头节点，该头节点在head节点中
             */
            Link link;


            /**
             * 构造方法
             *
             * @param availableSharedCapacity
             */
            Head(AtomicInteger availableSharedCapacity) {
                this.availableSharedCapacity = availableSharedCapacity;
            }


            /**
             * 该方法和垃圾回收有关，实际上做的工作就是在没清空一个link对象之后，给availableSharedCapacity加上
             * 一个LINK_CAPACITY值，也就是16，一个link的容量为16。然后把引用置空，帮助垃圾回收。
             *
             * @throws Throwable
             */
            @Override
            protected void finalize() throws Throwable {
                try {
                    super.finalize();
                } finally {
                    Link head = link;
                    link = null;
                    // 链表一个一个设置null，释放
                    while (head != null) {
                        // 每释放一个节点 availableSharedCapacity 就加 16
                        reclaimSpace(LINK_CAPACITY);
                        Link next = head.next;
                        head.next = null;
                        head = next;
                    }
                }
            }


            /**
             * 每释放一个link节点，就为该属性加上16，相反，每创建一个link节点，就要为该属性减16
             *
             * @param space
             */
            void reclaimSpace(int space) {
                assert space >= 0;
                availableSharedCapacity.addAndGet(space);
            }

            /**
             * 为availableSharedCapacity减16
             *
             * @param space
             * @return
             */
            boolean reserveSpace(int space) {
                //是个原子方法
                return reserveSpace(availableSharedCapacity, space);
            }

            /**
             * availableSharedCapacity是2048个，初始化好的。创建了一个队列，添加了一个link节点，所以要先减去16个容量
             * 等释放link节点后，可以再把16容量加回来
             *
             * @param availableSharedCapacity
             * @param space
             * @return
             */
            static boolean reserveSpace(AtomicInteger availableSharedCapacity, int space) {
                assert space >= 0;
                for (; ; ) {
                    //获取其他帮忙回收的线程可以为该线程回收availableSharedCapacity回收的值
                    int available = availableSharedCapacity.get();
                    //如果可帮助的对象个个数小于需要的对象个数，就直接返回false
                    if (available < space) {
                        return false;
                    }
                    //走到这里说明这个线程还可以继续为原本获得对象的线程回收对象，那就用原子方法减去16
                    if (availableSharedCapacity.compareAndSet(available, available - space)) {
                        return true;
                    }
                }
            }
        }

    }

}
