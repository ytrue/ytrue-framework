package com.ytrue.netty.util.concurrent;

import com.ytrue.netty.channel.EventLoopTaskQueueFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author ytrue
 * @date 2023-07-22 13:52
 * @description 单线程执行器，实际上这个类就是一个单线程的线程池，netty中所有任务都是被该执行器执行的
 * 既然是执行器(虽然该执行器中只有一个无限循环的线程工作)，但执行器应该具备的属性也不可少，比如任务队列，拒绝策略等等
 */
@Slf4j
public abstract class SingleThreadEventExecutor implements Executor {


    /**
     * 执行器的初始状态，未启动
     */
    private static final int ST_NOT_STARTED = 1;

    /**
     * 执行器启动后的状态
     */
    private static final int ST_STARTED = 2;

    /**
     * 线程的状态
     */
    private volatile int state = ST_NOT_STARTED;

    /**
     * 执行器的状态更新器,也是一个原子类，通过cas来改变执行器的状态值
     */
    private static final AtomicIntegerFieldUpdater<SingleThreadEventExecutor> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(SingleThreadEventExecutor.class, "state");

    /**
     * 任务队列的容量，默认是Integer的最大值
     */
    protected static final int DEFAULT_MAX_PENDING_TASKS = Integer.MAX_VALUE;


    /**
     * 任务队列
     */
    private final Queue<Runnable> taskQueue;


    /**
     * 创建这个执行器的线程
     */
    private volatile Thread thread;

    /**
     * 创建线程的执行器
     */
    private Executor executor;

    /**
     * 是否打断
     */
    private volatile boolean interrupted;


    /**
     * 队列满了拒绝策略
     */
    private final RejectedExecutionHandler rejectedExecutionHandler;

    /**
     * @param executor      执行器
     * @param queueFactory  队列工厂
     * @param threadFactory 线程工厂
     */
    protected SingleThreadEventExecutor(Executor executor, EventLoopTaskQueueFactory queueFactory, ThreadFactory threadFactory) {
        this(executor, queueFactory, threadFactory, RejectedExecutionHandlers.reject());
    }


    protected SingleThreadEventExecutor(Executor executor, EventLoopTaskQueueFactory queueFactory, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        if (executor == null) {
            this.executor = new ThreadPerTaskExecutor(threadFactory);
        }
        // 创建队列
        this.taskQueue = queueFactory == null ? newTaskQueue(DEFAULT_MAX_PENDING_TASKS) : queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
        // 赋值拒绝策略
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }


    /**
     * 创建任务队列
     *
     * @param maxPendingTasks
     * @return
     */
    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        return new LinkedBlockingQueue<>(maxPendingTasks);
    }

    /**
     * 该方法在nioeventloop中实现，是真正执行轮询的方法
     */
    protected abstract void run();

    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        //把任务提交到任务队列中
        addTask(task);
        //启动单线程执行器中的线程
        startThread();
    }

    /**
     * 启动线程
     */
    private void startThread() {
        //暂时先不考虑特别全面的线程池状态，只关心线程是否已经启动
        //如果执行器的状态是未启动，就cas将其状态值变为已动
        if (state == ST_NOT_STARTED) {
            if (STATE_UPDATER.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
                boolean success = false;

                try {
                    doStartThread();
                    success = true;
                } finally {
                    //如果启动未成功，直接把状态值复原
                    if (!success) {
                        STATE_UPDATER.compareAndSet(this, ST_STARTED, ST_NOT_STARTED);
                    }
                }
            }
        }
    }

    private void doStartThread() {
        //这里的executor是ThreadPerTaskExecutor，runnable -> threadFactory.newThread(command).start()
        //threadFactory中new出来的thread就是单线程线程池中的线程，它会调用nioeventloop中的run方法，无限循环，直到资源被释放
        executor.execute(() -> {
            //Thread.currentThread得到的就是正在执行任务的单线程执行器的线程，这里把它赋值给thread属性十分重要
            //暂时先记住这一点
            thread = Thread.currentThread();
            if (interrupted) {
                thread.interrupt();
            }
            //线程开始轮询处理IO事件，父类中的关键字this代表的是子类对象，这里调用的是nioeventloop中的run方法
            SingleThreadEventExecutor.this.run();
            log.info("单线程执行器的线程错误结束了！");
        });
    }


    /**
     * 执行任务
     *
     * @param task
     */
    private void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            log.warn("TASK RAISED AN Throwable Task {}", task, t);
        }
    }

    /**
     * 执行队列所有任务
     */
    protected void runAllTasks() {
        runAllTaskFrom(taskQueue);
    }

    /**
     * 执行对于队列的任务
     *
     * @param taskQueue
     */
    protected void runAllTaskFrom(Queue<Runnable> taskQueue) {
        // 从任务对立中拉取任务,如果第一次拉取就为null，说明任务队列中没有任务，直接返回即可
        Runnable task = pollTaskFrom(taskQueue);
        if (task == null) {
            return;
        }

        while (true) {
            // 执行任务队列中的任务
            safeExecute(task);
            // 执行完毕之后，拉取下一个任务，如果为null就直接返回
            task = pollTaskFrom(taskQueue);
            if (task == null) {
                return;
            }
        }
    }


    /**
     * 判断当前执行任务的线程是否是执行器的线程
     *
     * @param thread
     * @return
     */
    public boolean inEventLoop(Thread thread) {
        return thread == this.thread;
    }


    /**
     * 获取元素从队列
     *
     * @param taskQueue
     * @return
     */
    protected static Runnable pollTaskFrom(Queue<Runnable> taskQueue) {
        //  移除并返问队列头部的元素    如果队列为空，则返回null
        return taskQueue.poll();
    }

    /**
     * 是否有任务
     *
     * @return
     */
    protected boolean hasTasks() {
        return !taskQueue.isEmpty();
    }

    /**
     * 添加任务
     *
     * @param task
     */
    private void addTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        if (!offerTask(task)) {
            reject(task);
        }

    }

    /**
     * 添加元素到队列
     *
     * @param task
     * @return
     */
    protected boolean offerTask(Runnable task) {
        //  添加一个元素并返回true 如果队列已满，则返回false
        return taskQueue.offer(task);
    }


    /**
     * 直接抛出异常的拒绝策略
     */
    protected static void reject() {
        throw new RejectedExecutionException("event executor terminated");
    }


    /**
     * 拒绝策略
     *
     * @param task
     */
    protected void reject(Runnable task) {
        rejectedExecutionHandler.rejected(task, this);
    }

    /**
     * 打断线程
     */
    protected void interruptThread() {
        // 获取当前线程
        Thread currentThread = thread;
        // 如果当前线程等于null 直接设置打断了
        if (currentThread == null) {
            interrupted = true;
        } else {
            // 中断线程并不是直接让该线程停止运行，而是提供一个中断信号
            // 也就是标记，想要停止线程仍需要在运行流程中结合中断标记来判断
            currentThread.interrupt();
        }
    }

}
