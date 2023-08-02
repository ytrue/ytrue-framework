package com.ytrue.netty.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

import static com.ytrue.netty.util.internal.StringUtil.simpleClassName;

/**
 * @author ytrue
 * @date 2023-08-02 9:14
 * @description netty实现的时间轮是单线程的，这意味着一旦时间轮中的任务有阻塞的话，整个时间轮中任务的执行可能都会受到影响
 */
@Slf4j
public class HashedWheelTimer implements Timer {


    private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();
    private static final AtomicBoolean WARNED_TOO_MANY_INSTANCES = new AtomicBoolean();
    private static final int INSTANCE_COUNT_LIMIT = 64;

    /**
     * 1 纳秒
     */
    private static final long MILLISECOND_NANOS = TimeUnit.MILLISECONDS.toNanos(1);


    /**
     * 时间轮工作状态更新器，原子更新器
     */
    private static final AtomicIntegerFieldUpdater<HashedWheelTimer> WORKER_STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimer.class, "workerState");


    /**
     * 初始化该时间轮的runable，时间轮是单线程的，单线程执行的是该runnable，其中的run方法就是时间轮的核心逻辑
     */
    private final Worker worker = new Worker();


    /**
     * workerThread 单线程用于处理所有的定时任务，它会在每个tick执行一个bucket中所有的定时任务，
     * 以及一些其他的操作。意味着定时任务不能有较大的阻塞和耗时，不然就会影响定时任务执行的准时性和有效性。
     */
    private final Thread workerThread;


    /**
     * 下面三个属性就是状态值
     */
    public static final int WORKER_STATE_INIT = 0;
    public static final int WORKER_STATE_STARTED = 1;
    public static final int WORKER_STATE_SHUTDOWN = 2;


    /**
     * 0 是 init, 1 是 started, 2 是 shut down
     */
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private volatile int workerState;

    /**
     * 时间间隔
     */
    private final long tickDuration;

    /**
     * 时间轮数组，数组的每一个位置存放的是HashedWheelBucket类型的双向链表
     */
    private final HashedWheelBucket[] wheel;

    /**
     * 掩码，计算定时任务要存入的数组的下标
     */
    private final int mask;

    /**
     * 这个属性也很重要，在具体方法内会看到用处，先混个眼熟
     */
    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);

    /**
     * 本来在时间轮中用的是无锁队列，要引入新的jar包，其实性能也体现在这里。但这节课我们只是讲解时间轮的调用逻辑，所以就把这两个高性能
     * 无锁队列换成下面Java的队列了
     */
    private final Queue<HashedWheelTimeout> timeouts = new LinkedBlockingDeque<>();
    private final Queue<HashedWheelTimeout> cancelledTimeouts = new LinkedBlockingDeque<>();

    /**
     * 等待执行的定时任务的个数
     */
    private final AtomicLong pendingTimeouts = new AtomicLong(0);

    /**
     * 最大的任务数量。当HashedWheelTimer实例上的任务超出这个数量时会抛出错误
     */
    private final long maxPendingTimeouts;

    /**
     * 时间轮的启动时间
     */
    private volatile long startTime;


    public HashedWheelTimer() {
        this(Executors.defaultThreadFactory());
    }


    public HashedWheelTimer(long tickDuration, TimeUnit unit) {
        this(Executors.defaultThreadFactory(), tickDuration, unit);
    }


    public HashedWheelTimer(long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(Executors.defaultThreadFactory(), tickDuration, unit, ticksPerWheel);
    }


    public HashedWheelTimer(ThreadFactory threadFactory) {
        this(threadFactory, 100, TimeUnit.MILLISECONDS);
    }


    public HashedWheelTimer(
            ThreadFactory threadFactory, long tickDuration, TimeUnit unit) {
        this(threadFactory, tickDuration, unit, 512);
    }


    public HashedWheelTimer(
            ThreadFactory threadFactory,
            long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(threadFactory, tickDuration, unit, ticksPerWheel, true);
    }


    public HashedWheelTimer(
            ThreadFactory threadFactory,
            long tickDuration,
            TimeUnit unit,
            int ticksPerWheel,
            boolean leakDetection
    ) {
        this(threadFactory, tickDuration, unit, ticksPerWheel, leakDetection, -1);
    }


    /**
     * threadFactory是创建线程的工厂，tickDuration是tick的时长，也就是指针多久转一格，
     * tickDuration是时间间隔，也就是时间轮的刻度大小，比如是按一个小时划分刻度，
     * 还是按两个星期划分刻度，unit是刻度的单位，ticksPerWheel是时间轮的容量，也就是时间轮数组槽位的个数，leakDetection检查内存泄漏。
     * maxPendingTimeouts是最大任务数量
     *
     * @param threadFactory      线程工厂
     * @param tickDuration       时间间隔
     * @param unit               时间单位
     * @param ticksPerWheel      时间轮数组容量
     * @param leakDetection      检查内存泄漏
     * @param maxPendingTimeouts 最大任务数量
     */
    public HashedWheelTimer(
            ThreadFactory threadFactory,
            long tickDuration,
            TimeUnit unit,
            int ticksPerWheel,
            boolean leakDetection,
            long maxPendingTimeouts
    ) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
        }
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }

        //创建时间轮的数组，数组的长度也是有讲究的，必须是不小于ticksPerWheel的最小2的n次方，这和hashmap中一样，用位运算求下标
        wheel = createWheel(ticksPerWheel);
        //掩码，计算定时任务要存放的数组下标
        mask = wheel.length - 1;


        //时间换算成纳秒
        long duration = unit.toNanos(tickDuration);
        //时间间隔不能太长
        if (duration >= Long.MAX_VALUE / wheel.length) {
            throw new IllegalArgumentException(String.format("tickDuration: %d (expected: 0 < tickDuration in nanos < %d", tickDuration, Long.MAX_VALUE / wheel.length));
        }

        //时间间隔不能太短，至少要大于1纳秒
        if (duration < MILLISECOND_NANOS) {
            if (log.isWarnEnabled()) {
                log.warn("Configured tickDuration %d smaller then %d, using 1ms.",
                        tickDuration, MILLISECOND_NANOS);
            }
            this.tickDuration = MILLISECOND_NANOS;
        } else {
            this.tickDuration = duration;
        }

        //创建工作线程
        workerThread = threadFactory.newThread(worker);

        // 内存泄漏检测，暂时不引入
        //leak = leakDetection || !workerThread.isDaemon() ? leakDetector.track(this) : null;
        //当时间轮上的定时任务数量超过该值就报警
        this.maxPendingTimeouts = maxPendingTimeouts;

        // 这里的意思是如果创建的时间轮对象超过64个，也会报警。一个时间轮就是一个线程，线程太多也会影响性能
        if (INSTANCE_COUNTER.incrementAndGet() > INSTANCE_COUNT_LIMIT &&
                WARNED_TOO_MANY_INSTANCES.compareAndSet(false, true)) {
            reportTooManyInstances();
        }
    }


    /**
     * 时间轮实例太多就报告一下，打印日志
     */
    private static void reportTooManyInstances() {
        if (log.isErrorEnabled()) {
            String resourceType = simpleClassName(HashedWheelTimer.class);
            log.error("You are creating too many " + resourceType + " instances. " +
                    resourceType + " is a shared resource that must be reused across the JVM," +
                    "so that only a few instances are created.");
        }
    }

    /**
     * 创建时间轮数组
     *
     * @param ticksPerWheel
     * @return
     */
    private static HashedWheelBucket[] createWheel(int ticksPerWheel) {
        //时间轮太小就抛出异常
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException(
                    "ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }
        if (ticksPerWheel > 1073741824) {
            throw new IllegalArgumentException(
                    "ticksPerWheel may not be greater than 2^30: " + ticksPerWheel);
        }
        //把时间轮数组长度设定到2的次方
        ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
        //初始化每一个位置的bucket
        HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new HashedWheelBucket();
        }
        return wheel;
    }

    /**
     * 长度设置到2的N次方
     *
     * @param ticksPerWheel
     * @return
     */
    private static int normalizeTicksPerWheel(int ticksPerWheel) {
        int normalizedTicksPerWheel = 1;
        while (normalizedTicksPerWheel < ticksPerWheel) {
            normalizedTicksPerWheel <<= 1;
        }
        return normalizedTicksPerWheel;
    }


    @Override
    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {
                INSTANCE_COUNTER.decrementAndGet();
            }
        }
    }

    /**
     * 启动时间轮的方法
     */
    public void start() {
        //判断时间轮的工作状态
        switch (WORKER_STATE_UPDATER.get(this)) {
            case WORKER_STATE_INIT:
                //cas更新到开始状态
                if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
                    //启动work线程，该线程一旦启动，就会执行任务，所以核心在work线程要执行的runable的run方法内
                    workerThread.start();
                }
                break;
            //如果启动了就什么也不做
            case WORKER_STATE_STARTED:
                break;
            //如果状态是结束，就抛出异常
            case WORKER_STATE_SHUTDOWN:
                throw new IllegalStateException("cannot be started once stopped");
            default:
                throw new Error("Invalid WorkerState");
        }
        //这里会暂停一卡，因为要等待work线程启动完全，并且starttime被赋值成功
        while (startTime == 0) {
            try {
                startTimeInitialized.await();
            } catch (InterruptedException ignore) {
                // Ignore - it will be ready very soon.
            }
        }
    }


    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        //添加任务之后，等待执行的任务加1
        long pendingTimeoutsCount = pendingTimeouts.incrementAndGet();
        //如果等待执行的任务超过时间轮能处理的最大任务数，就直接报错
        if (maxPendingTimeouts > 0 && pendingTimeoutsCount > maxPendingTimeouts) {
            pendingTimeouts.decrementAndGet();
            throw new RejectedExecutionException("Number of pending timeouts ("
                    + pendingTimeoutsCount + ") is greater than or equal to maximum allowed pending "
                    + "timeouts (" + maxPendingTimeouts + ")");
        }
        //启动工作线程，并且确保只启动一次，这里面会设计线程的等待和唤醒
        start();
        //计算该定时任务的执行时间，startTime是worker线程的开始时间。以后所有添加进来的任务的执行时间，都是根据这个开始时间做的对比
        long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;
        //检查时间间隔
        if (delay > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }
        //把提交的任务封装进一个HashedWheelTimeout中。
        HashedWheelTimeout timeout = new HashedWheelTimeout(this, task, deadline);
        //将定时任务添加到任务队列中
        timeouts.add(timeout);
        return timeout;
    }


    /**
     * 时间轮的停止方法
     *
     * @return
     */
    @Override
    public Set<Timeout> stop() {
        //判断当前线程是否是worker线程，不能让本身去停止本身
        if (Thread.currentThread() == workerThread) {
            throw new IllegalStateException(
                    HashedWheelTimer.class.getSimpleName() +
                            ".stop() cannot be called from " +
                            TimerTask.class.getSimpleName());
        }
        //cas更新状态，如果更新不成功返回false，取反就是true，就会进入if分支
        if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
            if (WORKER_STATE_UPDATER.getAndSet(this, WORKER_STATE_SHUTDOWN) != WORKER_STATE_SHUTDOWN) {
                //停止了一个时间轮，时间轮的个数就要减1
                INSTANCE_COUNTER.decrementAndGet();
//                if (leak != null) {
//                    内存溢出先不考虑
//                    boolean closed = leak.close(this);
//                    assert closed;
//                }
            }

            return Collections.emptySet();
        }

        try {
            //来到了这里，说明之前cas更新成功
            boolean interrupted = false;
            //while循环持续中断worker线程，isAlive用来判断该线程是否结束
            while (workerThread.isAlive()) {
                workerThread.interrupt();
                try {
                    //work线程阻塞的同时又被中断了，会抛出异常
                    workerThread.join(100);
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                //给执行该方法的线程设置中断标志位
                Thread.currentThread().interrupt();
            }
        } finally {
            //减少实例数
            INSTANCE_COUNTER.decrementAndGet();
//            内存溢出先不考虑
//            if (leak != null) {
//                boolean closed = leak.close(this);
//                assert closed;
//            }
        }
        //返回还没执行的定时任务的集合
        return worker.unprocessedTimeouts();
    }

    private final class Worker implements Runnable {

        /**
         * 未处理的超时
         */
        private final Set<Timeout> unprocessedTimeouts = new HashSet<>();

        /**
         * 这个属性代表当前时间轮的指针移动了几个刻度
         */
        private long tick;

        @Override
        public void run() {
            // startTime 时间轮启动的时间， 给startTime赋值，这里要等待该值复制成功后，另一个线程才能继续向下执行·
            startTime = System.nanoTime();
            if (startTime == 0) {
                //System.nanoTime()可能返回0，也可能是负数。
                //所以用0来当作一个标识符。当startTime=0的时候，就把startTime赋值为1
                startTime = 1;
            }

            // 这里是不是就串联起来了，通知之前的线程可以继续向下运行了
            // 对计数器进行递减1操作，当计数器递减至0时，当前线程会去唤醒阻塞队列里的所有线程。
            startTimeInitialized.countDown();

            do {
                // 返回的是work线程从开始工作到现在执行了多少时间了
                final long deadline = waitForNextTick();
                if (deadline > 0) {
                    //获取要执行的定时任务的那个数组下标。就是让指针当前的刻度和掩码做位运算
                    int idx = (int) (tick & mask);
                    //如果有任务已经被取消了，先把这些任务处理一下
                    processCancelledTasks();
                    //上面已经得到了要执行的定时任务的数组下标，这里就可以得到该bucket，而这个bucket就是定时任务的一个双向链表
                    //链表中的每个节点都是一个定时任务
                    HashedWheelBucket bucket = wheel[idx];
                    //在真正执行定时任务之前，把即将被执行的任务从队列中放到时间轮的数组当中
                    transferTimeoutsToBuckets();
                    //执行定时任务
                    bucket.expireTimeouts(deadline);
                    //指针已经移动了，所以加1
                    tick++;
                }
                //时间轮状态是开始执行状态就一直循环 , workerState 等于 开始的 就一直执行
            } while (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_STARTED);

            //走到这里，说明时间轮的状态已经改变了
            //遍历所有的bucket，还没被处理的定时任务都放到队列中
            for (HashedWheelBucket bucket : wheel) {
                bucket.clearTimeouts(unprocessedTimeouts);
            }
            for (; ; ) {
                //这里遍历的是任务队列中的任务，这些任务还没被放进时间轮数组中，将这些任务也都放进一个任务队列中
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    break;
                }
                if (!timeout.isCancelled()) {
                    unprocessedTimeouts.add(timeout);
                }
            }
            //如果有定时任务被取消了，在这里把它们从链表中删除
            processCancelledTasks();
        }


        /**
         * 该方法就是要把任务队列中的定时任务转移到时间轮的数组当中
         */
        private void transferTimeoutsToBuckets() {
            //限制最多一次转移100000个，转移太多线程就干不了别的活了
            for (int i = 0; i < 100000; i++) {
                //获取任务队列中的首个定时任务
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    break;
                }
                //如果该任务已经被取消了，就不转移该任务
                if (timeout.state() == HashedWheelTimeout.ST_CANCELLED) {
                    continue;
                }
                //计算从worker线程开始运行算起要经过多少个tick，也就是刻度才能到这个任务，如果10秒执行，每一个刻度是 1秒 那么这里就是 10个刻度
                // timeout.deadline  = 开始时间 + 延后的时间
                long calculated = timeout.deadline / tickDuration;

                //计算这个任务要经过多少圈，这里为什么要减tick，其实很简单，就是减去work线程已经走过的刻度   10个刻度 - 当前走了多少刻度 / 桶的长度 就得出 要转多少圈
                timeout.remainingRounds = (calculated - tick) / wheel.length;
                //通常calculated是大于tick的
                //如果某些任务执行时间过长，导致tick大于calculated，此时直接把过时的任务放到当前链表队列
                final long ticks = Math.max(calculated, tick);

                //位运算计算出该定时任务应该放在的数组下标
                int stopIndex = (int) (ticks & mask);
                //得到数组下标中的bucket节点
                HashedWheelBucket bucket = wheel[stopIndex];
                //把定时任务添加到链表之中
                bucket.addTimeout(timeout);
            }
        }

        /**
         * 处理已经被取消了的定时任务
         */
        private void processCancelledTasks() {
            //这里没有数量限制，是考虑到取消的定时任务不会很多
            for (; ; ) {
                HashedWheelTimeout timeout = cancelledTimeouts.poll();
                if (timeout == null) {
                    break;
                }
                try {
                    //从bucket双向链表中把该任务删除，后面就会看到了
                    timeout.remove();
                } catch (Throwable t) {
                    if (log.isWarnEnabled()) {
                        log.warn("An exception was thrown while process a cancellation task", t);
                    }
                }
            }
        }

        private long waitForNextTick() {
            // 获取下一个指针的执行时间，如果tick是0，说明时间轮可能是刚开始，接下来就要执行1刻度的定时任务
            // 而到达1刻度要经过的时间，就是下面这样计算的
            // tickDuration 时间间隔
            // tick 这个属性代表当前时间轮的指针移动了几个刻度
            long deadline = tickDuration * (tick + 1);

            for (; ; ) {
                //这个时间实际上是work线程工作了多久的时间   10,  1  15  = 16
                final long currentTime = System.nanoTime() - startTime;

                //这里加上999999的是因为除法只会取整数部分，为了保证任务不被提前执行，加上999999后就能够向上取整1ms。
                //deadline - currentTime = 3213213，得到的是个3.几的数。
                //还有多久要执行下一个刻度的定时任务，用这个时间减去work线程已经运行的时间，得到的就是work线程还要经过多久才会
                //执行到下一个刻度的定时任务
                long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

                //小于0则代表到了定时任务的执行时间
                if (sleepTimeMs <= 0) {
                    if (currentTime == Long.MIN_VALUE) {
                        return -Long.MAX_VALUE;
                    } else {
                        //返回work线程工作的时间
                        return currentTime;
                    }
                }
                try {
                    //走到这里就意味着还没到执行时间，需要睡一会才行
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException ignored) {
                    // 如果时间轮已经shutdown了，则返回MIN_VALUE
                    if (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_SHUTDOWN) {
                        return Long.MIN_VALUE;
                    }
                }
            }
        }

        public Set<Timeout> unprocessedTimeouts() {
            return Collections.unmodifiableSet(unprocessedTimeouts);
        }
    }

    /**
     * 封装了用户提交的定时任务，而且该类的对象构成了bucket的双向链表
     */
    private static final class HashedWheelTimeout implements Timeout {


        /**
         * 下面三个是该Timeout的状态，字面意思直接翻译即可
         * 初始化
         */
        private static final int ST_INIT = 0;

        /**
         * 取消
         */
        private static final int ST_CANCELLED = 1;

        /**
         * 过期
         */
        private static final int ST_EXPIRED = 2;

        /**
         * cas 修改状态
         */
        private static final AtomicIntegerFieldUpdater<HashedWheelTimeout> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimeout.class, "state");


        private final HashedWheelTimer timer;
        private final TimerTask task;
        private final long deadline;


        /**
         * 任务状态，默认是初始化
         */
        @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization"})
        private volatile int state = ST_INIT;


        /**
         * 定时任务的轮数，因为这个定时任务可能要时间轮走两轮才执行，那么每走一轮，该值就减1，为0就说明到了可以执行的轮数了
         */
        long remainingRounds;

        /**
         * 双向链表结构，由于只有worker线程会访问，这里不需要synchronization/volatile
         * 这里的链表结构，就是因为该对象在bucket中是会构成链表的节点。
         */
        HashedWheelTimeout next;
        HashedWheelTimeout prev;

        /**
         * 定时任务所在的时间轮的位置
         */
        HashedWheelBucket bucket;


        /**
         * 构造
         *
         * @param timer
         * @param task
         * @param deadline
         */
        HashedWheelTimeout(HashedWheelTimer timer, TimerTask task, long deadline) {
            this.timer = timer;
            this.task = task;
            this.deadline = deadline;
        }

        @Override
        public Timer timer() {
            return timer;
        }

        @Override
        public TimerTask task() {
            return task;
        }

        @Override
        public boolean isExpired() {
            return state() == ST_EXPIRED;
        }

        @Override
        public boolean isCancelled() {
            return state() == ST_CANCELLED;
        }

        @Override
        public boolean cancel() {
            // 设置取消
            if (!compareAndSetState(ST_INIT, ST_CANCELLED)) {
                return false;
            }
            //把取消的定时任务放进代表取消的任务队列中
            timer.cancelledTimeouts.add(this);
            return true;
        }


        /**
         * cas 设置状态
         *
         * @param expected
         * @param state
         * @return
         */
        public boolean compareAndSetState(int expected, int state) {
            return STATE_UPDATER.compareAndSet(this, expected, state);
        }

        public int state() {
            return state;
        }


        /**
         * 删除定时任务的方法，这里其实是找到该定时任务所在的bucket，然后在bucket的双向链表中删除
         */
        void remove() {
            HashedWheelBucket bucket = this.bucket;
            if (bucket != null) {
                bucket.remove(this);
            } else {
                // 等待执行的定时任务的个数 - 1
                timer.pendingTimeouts.decrementAndGet();
            }
        }


        /**
         * 到期，执行任务
         */
        public void expire() {
            if (!compareAndSetState(ST_INIT, ST_EXPIRED)) {
                return;
            }
            try {
                task.run(this);
            } catch (Throwable t) {
                if (log.isWarnEnabled()) {
                    log.warn("An exception was thrown by " + TimerTask.class.getSimpleName() + '.', t);
                }
            }
        }

        @Override
        public String toString() {
            final long currentTime = System.nanoTime();
            long remaining = deadline - currentTime + timer.startTime;

            StringBuilder buf = new StringBuilder(192).append(simpleClassName(this)).append('(').append("deadline: ");
            if (remaining > 0) {
                buf.append(remaining).append(" ns later");
            } else if (remaining < 0) {
                buf.append(-remaining).append(" ns ago");
            } else {
                buf.append("now");
            }

            if (isCancelled()) {
                buf.append(", cancelled");
            }

            return buf.append(", task: ").append(task()).append(')').toString();
        }
    }


    /**
     * 时间轮数组每一个位置存放的对象
     */
    private static final class HashedWheelBucket {
        /**
         * 头部
         */
        private HashedWheelTimeout head;

        /**
         * 尾部
         */
        private HashedWheelTimeout tail;


        /**
         * 在这个方法中，timeout中的bucket属性被赋值了
         *
         * @param timeout
         */
        public void addTimeout(HashedWheelTimeout timeout) {
            assert timeout.bucket == null;
            timeout.bucket = this;
            if (head == null) {
                //这里虽然是头尾节点，但实际上添加第一个节点的时候，头节点和为节点和添加的节点就变成了同一个节点
                head = tail = timeout;
            } else {
                tail.next = timeout;
                timeout.prev = tail;
                tail = timeout;
            }
        }


        /**
         * 把定时任务从双向链表中删除
         *
         * @param timeout
         * @return
         */
        public HashedWheelTimeout remove(HashedWheelTimeout timeout) {
            HashedWheelTimeout next = timeout.next;
            if (timeout.prev != null) {
                timeout.prev.next = next;
            }
            if (timeout.next != null) {
                timeout.next.prev = timeout.prev;
            }
            if (timeout == head) {
                if (timeout == tail) {
                    tail = null;
                    head = null;
                } else {
                    head = next;
                }
            } else if (timeout == tail) {
                tail = timeout.prev;
            }
            timeout.prev = null;
            timeout.next = null;
            timeout.bucket = null;
            timeout.timer.pendingTimeouts.decrementAndGet();
            return next;
        }

        public void clearTimeouts(Set<Timeout> set) {
            for (; ; ) {
                HashedWheelTimeout timeout = pollTimeout();
                if (timeout == null) {
                    return;
                }
                if (timeout.isExpired() || timeout.isCancelled()) {
                    continue;
                }
                set.add(timeout);
            }
        }

        /**
         * 弹出第一个 元素
         *
         * @return
         */
        private HashedWheelTimeout pollTimeout() {
            HashedWheelTimeout head = this.head;
            if (head == null) {
                return null;
            }
            HashedWheelTimeout next = head.next;
            if (next == null) {
                tail = this.head = null;
            } else {
                this.head = next;
                next.prev = null;
            }
            //帮助垃圾回收
            head.next = null;
            head.prev = null;
            head.bucket = null;
            return head;
        }


        /**
         * 执行定时任务的方法，传进来的参数是到期的时间
         *
         * @param deadline
         */
        public void expireTimeouts(long deadline) {
            //获取链表的头节点，注意啊，这时候已经定位到了具体的bucket了
            HashedWheelTimeout timeout = head;
            //开始处理定时任务
            while (timeout != null) {
                //先得到下一个定时任务
                HashedWheelTimeout next = timeout.next;
                //剩余轮数小于0就说明这一轮就可以执行该定时任务了
                if (timeout.remainingRounds <= 0) {
                    //把该定时任务从双向链表中删除，该方法的返回结果是下一个节点
                    next = remove(timeout);
                    if (timeout.deadline <= deadline) {
                        //执行定时任务
                        timeout.expire();
                    } else {
                        throw new IllegalStateException(String.format("timeout.deadline (%d) > deadline (%d)", timeout.deadline, deadline));
                    }
                } else if (timeout.isCancelled()) {
                    //如果定时任务被取消了，从双向链表中删除该任务
                    next = remove(timeout);
                } else {
                    //走到这里说明remainingRounds还大于0，这就意味着还不到执行定时任务的轮数，轮数减1即可
                    timeout.remainingRounds--;
                }
                //向后遍历双向链表
                timeout = next;
            }
        }
    }
}
