package com.ytrue.netty.util.concurrent;

import com.ytrue.netty.util.internal.DefaultPriorityQueue;
import com.ytrue.netty.util.internal.PriorityQueueNode;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ytrue
 * @date 2023-07-31 9:52
 * @description ScheduledFutureTask 时间轮算法
 */
public class ScheduledFutureTask<V> extends PromiseTask<V> implements ScheduledFuture<V>, PriorityQueueNode {

    /**
     * 下一个任务的id，每一个任务都有一个对应的id
     */
    private static final AtomicLong nextTaskId = new AtomicLong();

    /**
     * 任务的创建时间，当第一个任务被创建的时候，随着ScheduledFutureTask类的初始化，该属性也被初始化了，并且只初始化一次
     */
    private static final long START_TIME = System.nanoTime();


    //给该任务的id赋值
    private final long id = nextTaskId.getAndIncrement();

    /**
     * 这个就是该任务要被执行的时间
     */
    private long deadlineNanos;

    /**
     * 翻译过来是周期的意思，这个属性自然会和循环执行的定时任务有关。当它是0的时候，该任务不会循环执行
     * 大于0的时候则对应scheduleAtFixedRate
     * 小于0则对应scheduleWithFixedDelay
     */
    private final long periodNanos;

    /**
     * 该属性就是定时任务在任务队列中的下标，默认是-1，因为这时候定时任务还未被添加到任务队列中
     */
    private int queueIndex = INDEX_NOT_IN_QUEUE;

    /**
     * 当前时间减去开始时间
     *
     * @return
     */
    static long nanoTime() {
        return System.nanoTime() - START_TIME;
    }


    /**
     * 获得定时任务的截止时间
     *
     * @param delay
     * @return
     */
    static long deadlineNanos(long delay) {
        //比如用户设定了一个每隔两秒就执行一次，并且在3秒后执行的定时任务，而我的定时任务是在第0秒的时候开始的，现在是3秒
        //下面的计算方法，就是让当前时间3减去开始的第0秒，然后再加上延时的3秒，也就是第6秒就是我的定时任务的截止时期，在第六秒，我的定时任务就要
        //开始执行了
        long deadlineNanos = nanoTime() + delay;
        //如果计算出错，就用下面的方法给deadlineNanos赋值
        return deadlineNanos < 0 ? Long.MAX_VALUE : deadlineNanos;
    }


    /**
     * 下面三个是构造方法
     *
     * @param executor
     * @param runnable
     * @param result
     * @param nanoTime
     */
    ScheduledFutureTask(AbstractScheduledEventExecutor executor, Runnable runnable, V result, long nanoTime) {
        // 这个nanoTime参数需要调用deadlineNanos方法来确定，而deadlineNanos方法在AbstractScheduledEventExecutor类中被调用了
        // 该构造器创建的定时任务只执行一次
        this(executor, toCallable(runnable, result), nanoTime);
    }

    ScheduledFutureTask(AbstractScheduledEventExecutor executor, Callable<V> callable, long nanoTime, long period) {
        super(executor, callable);
        if (period == 0) {
            throw new IllegalArgumentException("period: 0 (expected: != 0)");
        }
        deadlineNanos = nanoTime;
        //传进来的周期参数赋值给该成员变量
        periodNanos = period;
    }

    ScheduledFutureTask(AbstractScheduledEventExecutor executor, Callable<V> callable, long nanoTime) {
        super(executor, callable);
        deadlineNanos = nanoTime;
        //在这里属性被赋值了，0就代表着该定时任务不会被重复执行
        periodNanos = 0;
    }


    /**
     * 获取定时任务的执行器
     *
     * @return
     */
    @Override
    protected EventExecutor executor() {
        return super.executor();
    }

    /**
     * 获取定时任务的执行时间
     *
     * @return
     */
    public long deadlineNanos() {
        return deadlineNanos;
    }


    /**
     * 获取到该任务下一次执行的时间，注意这里得到的是个时间差
     *
     * @return
     */
    public long delayNanos() {
        return Math.max(0, deadlineNanos() - nanoTime());
    }

    public long delayNanos(long currentTimeNanos) {
        return Math.max(0, deadlineNanos() - (currentTimeNanos - START_TIME));
    }

    /**
     * 核心方法，任务调用逻辑就在这里面
     */
    @Override
    public void run() {
        //断言，确保执行定时任务的一定是单线程执行器的线程
        assert executor().inEventLoop(Thread.currentThread());
        try {
            // 如果该属性为0，则任务不会循环执行
            // 周期的意思，这个属性自然会和循环执行的定时任务有关。当它是0的时候，该任务不会循环执行
            if (periodNanos == 0) {
                //设置当前任务为不可取消
                if (setUncancellableInternal()) {
                    //执行定时任务
                    V result = task.call();
                    //执行成功之后赋值
                    setSuccessInternal(result);
                }
            } else {
                //走到这里说明任务会循环执行，先判断任务是否被取消了
                if (!isCancelled()) {
                    //执行任务，循环执行，没有返回值
                    task.call();
                    //判断当前的执行器是否已经关闭,该方法在这里还没被实现。
                    if (!executor().isShutdown()) {
                        //将当前的周期值赋值给p
                        long p = periodNanos;
                        if (p > 0) {
                            //周期时间加上定时任务的执行时间，这里得到的就是循环定时任务下一次的执行时间
                            //因为大于0的时候对应cheduleAtFixedRate，该方法的下次执行时间就是这次的执行时间加上固定周期
                            //如果延时了，那任务结束后并且超过下一次定时任务执行的时间了，那该定时任务就会立刻被执行
                            //上一次的定时任务执行时间加上固定的周期。
                            deadlineNanos += p;
                        } else {
                            //小于0则意味着对应scheduleWithFixedDelay方法，上一次任务结束后，如果超时了
                            //它会再次延时固定的周期，然后再执行下一个任务
                            //这里p本身就是-数，相减为加
                            //当前时间减去开始时间，然后加上固定的延时，这就意味着不管任务是否超时，都要加上延时
                            deadlineNanos = nanoTime() - p;
                        }
                        //如果该任务还没被取消
                        if (!isCancelled()) {
                            //得到定时任务的队列，把定时任务放进队列中，因为该任务是循环执行的，计算出了下一次的执行时间了，所以要重新放回
                            //队列中
                            Queue<ScheduledFutureTask<?>> scheduledTaskQueue = ((AbstractScheduledEventExecutor) executor()).scheduledTaskQueue;
                            assert scheduledTaskQueue != null;
                            scheduledTaskQueue.add(this);
                        }
                    }
                }
            }
        } catch (Throwable cause) {
            //出现异常则设置为失败
            setFailureInternal(cause);
        }
    }


    /**
     * 时间转换
     *
     * @param unit the time unit
     * @return
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delayNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * 比较器的具体使用逻辑，就是比较两个定时任务执行时间的大小，谁的时间小，谁就排在任务队列的前面
     *
     * @param o the object to be compared.
     * @return
     */
    @Override
    public int compareTo(Delayed o) {
        if (this == o) {
            return 0;
        }
        ScheduledFutureTask<?> that = (ScheduledFutureTask<?>) o;
        //获取执行时间的差值
        long d = deadlineNanos() - that.deadlineNanos();
        if (d < 0) {
            return -1;
        } else if (d > 0) {
            return 1;
            //根据id也可以对比，谁的id小，谁就是先被创建的，到这里就意味着两个定时任务的执行时期相等
        } else if (id < that.id) {
            return -1;
        } else if (id == that.id) {
            throw new Error();
        } else {
            return 1;
        }
    }


    /**
     * 取消该任务，并且要从任务队列中移除
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     *                              task should be interrupted; otherwise, in-progress tasks are allowed
     *                              to complete
     * @return
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean canceled = super.cancel(mayInterruptIfRunning);
        if (canceled) {
            ((AbstractScheduledEventExecutor) executor()).removeScheduled(this);
        }
        return canceled;
    }

    /**
     * 取消该任务，但不从任务队列中移除
     *
     * @param mayInterruptIfRunning
     * @return
     */
    boolean cancelWithoutRemove(boolean mayInterruptIfRunning) {
        return super.cancel(mayInterruptIfRunning);
    }


    @Override
    protected StringBuilder toStringBuilder() {
        StringBuilder buf = super.toStringBuilder();
        buf.setCharAt(buf.length() - 1, ',');
        return buf.append(" id: ")
                .append(id)
                .append(", deadline: ")
                .append(deadlineNanos)
                .append(", period: ")
                .append(periodNanos)
                .append(')');
    }

    @Override
    public int priorityQueueIndex(DefaultPriorityQueue<?> queue) {
        return queueIndex;
    }

    @Override
    public void priorityQueueIndex(DefaultPriorityQueue<?> queue, int i) {
        queueIndex = i;
    }
}
