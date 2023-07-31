package com.ytrue.netty.util.concurrent;

import com.ytrue.netty.util.internal.DefaultPriorityQueue;
import com.ytrue.netty.util.internal.ObjectUtil;
import com.ytrue.netty.util.internal.PriorityQueue;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * @author ytrue
 * @date 2023-07-31 9:51
 * @description AbstractScheduledEventExecutor
 */
public abstract class AbstractScheduledEventExecutor extends AbstractEventExecutor {

    /**
     * 该成员变量是一个比较器，通过task的到期时间比较大小。谁的到期时间长谁就大
     */
    private static final Comparator<ScheduledFutureTask<?>> SCHEDULED_FUTURE_TASK_COMPARATOR = ScheduledFutureTask::compareTo;


    /**
     * 定时任务队列
     */
    PriorityQueue<ScheduledFutureTask<?>> scheduledTaskQueue;


    /**
     * 构造
     */
    protected AbstractScheduledEventExecutor() {
    }

    /**
     * 构造
     *
     * @param parent
     */
    protected AbstractScheduledEventExecutor(EventExecutorGroup parent) {
        super(parent);
    }

    /**
     * 当前时间减去开始时间
     *
     * @return
     */
    protected static long nanoTime() {
        return ScheduledFutureTask.nanoTime();
    }


    /**
     * 得到存储定时任务的任务队列，可以看到其实现实际上是一个优先级队列
     *
     * @return
     */
    PriorityQueue<ScheduledFutureTask<?>> scheduledTaskQueue() {
        // 如果为空就创建一个新的,队列数组容量11
        if (scheduledTaskQueue == null) {
            // 这里把定义好的比较器SCHEDULED_FUTURE_TASK_COMPARATOR传进去了
            scheduledTaskQueue = new DefaultPriorityQueue<>(SCHEDULED_FUTURE_TASK_COMPARATOR, 11);
        }
        return scheduledTaskQueue;
    }

    /**
     * 判断认为队列是否为空
     *
     * @param queue
     * @return
     */
    private static boolean isNullOrEmpty(Queue<ScheduledFutureTask<?>> queue) {
        return queue == null || queue.isEmpty();
    }

    /**
     * 取消任务队列中的所有任务
     */
    protected void cancelScheduledTasks() {
        assert inEventLoop(Thread.currentThread());
        //得到任务队列
        PriorityQueue<ScheduledFutureTask<?>> scheduledTaskQueue = this.scheduledTaskQueue;
        // 如果为空，就不执行了
        if (isNullOrEmpty(scheduledTaskQueue)) {
            return;
        }

        //把任务队列转换成数组
        final ScheduledFutureTask<?>[] scheduledTasks = scheduledTaskQueue.toArray(new ScheduledFutureTask<?>[0]);
        //依次取消任务，该方法最终回调用到promise中
        for (ScheduledFutureTask<?> task : scheduledTasks) {
            task.cancelWithoutRemove(false);
        }
        //清空数组，实际上只是把size置为0了
        scheduledTaskQueue.clearIgnoringIndexes();
    }

    /**
     * 该方法用来获取即将可以执行的定时任务
     *
     * @return
     */
    protected final Runnable pollScheduledTask() {
        return pollScheduledTask(nanoTime());
    }

    /**
     * 该方法用来获取即将可以执行的定时任务
     *
     * @param nanoTime
     * @return
     */
    protected final Runnable pollScheduledTask(long nanoTime) {
        assert inEventLoop(Thread.currentThread());
        //得到任务队列
        Queue<ScheduledFutureTask<?>> scheduledTaskQueue = this.scheduledTaskQueue;
        //从任务队列中取出首元素
        ScheduledFutureTask<?> scheduledTask = scheduledTaskQueue == null ? null : scheduledTaskQueue.peek();
        if (scheduledTask == null) {
            return null;
        }

        //如果首任务符合被执行的条件，就将该任务返回
        // 如果task预期执行执行的时间，小于等于 nanoTime 就是可以执行的
        if (scheduledTask.deadlineNanos() <= nanoTime) {
            // 从队列中删除
            scheduledTaskQueue.remove();
            // 返回
            return scheduledTask;
        }
        return null;
    }


    /**
     * 距离下一个任务执行的时间
     *
     * @return
     */
    protected final long nextScheduledTaskNano() {
        Queue<ScheduledFutureTask<?>> scheduledTaskQueue = this.scheduledTaskQueue;
        //获取任务队列的头元素
        ScheduledFutureTask<?> scheduledTask = scheduledTaskQueue == null ? null : scheduledTaskQueue.peek();
        if (scheduledTask == null) {
            return -1;
        }
        //用该任务的到期时间减去当前事件
        return Math.max(0, scheduledTask.deadlineNanos() - nanoTime());
    }


    /**
     * 获取队列第一一个任务
     *
     * @return
     */
    final ScheduledFutureTask<?> peekScheduledTask() {
        Queue<ScheduledFutureTask<?>> scheduledTaskQueue = this.scheduledTaskQueue;
        if (scheduledTaskQueue == null) {
            return null;
        }
        //获取头部元素
        return scheduledTaskQueue.peek();
    }

    /**
     * 该方法会在NioEventLoop中被调用，用来判断是否存在已经到期了的定时任务。实际上就是得到定时任务队列中的首任务判断其是否可以被执行了
     *
     * @return
     */
    protected final boolean hasScheduledTasks() {
        Queue<ScheduledFutureTask<?>> scheduledTaskQueue = this.scheduledTaskQueue;
        ScheduledFutureTask<?> scheduledTask = scheduledTaskQueue == null ? null : scheduledTaskQueue.peek();

        return scheduledTask != null && scheduledTask.deadlineNanos() <= nanoTime();
    }


    /**
     * 提交普通的定时任务到任务队列中
     *
     * @param command the task to execute
     * @param delay   the time from now to delay execution
     * @param unit    the time unit of the delay parameter
     * @return
     */
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        ObjectUtil.checkNotNull(command, "command");
        ObjectUtil.checkNotNull(unit, "unit");
        if (delay < 0) {
            delay = 0;
        }
        validateScheduled0(delay, unit);
        return schedule(new ScheduledFutureTask<Void>(
                this,
                command,
                null,
                ScheduledFutureTask.deadlineNanos(unit.toNanos(delay)
                )));
    }

    /**
     * 提交普通的定时任务到任务队列
     *
     * @param callable the function to execute
     * @param delay    the time from now to delay execution
     * @param unit     the time unit of the delay parameter
     * @param <V>
     * @return
     */
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        ObjectUtil.checkNotNull(callable, "callable");
        ObjectUtil.checkNotNull(unit, "unit");
        if (delay < 0) {
            delay = 0;
        }
        validateScheduled0(delay, unit);

        return schedule(new ScheduledFutureTask<V>(
                this, callable, ScheduledFutureTask.deadlineNanos(unit.toNanos(delay))));
    }


    /**
     * 方法会按照指定的固定速率执行任务。
     * 它会在每次任务执行完成后，等待指定的时间间隔，然后再次执行下一个任务。
     * 如果任务的执行时间超过了指定的时间间隔，那么下一个任务将会立即开始执行，
     * 不会等待上一个任务的完成。这意味着任务的执行时间可能会与指定的时间间隔有所偏差。
     *
     * @param command      the task to execute
     * @param initialDelay the time to delay first execution
     * @param period       the period between successive executions
     * @param unit         the time unit of the initialDelay and period parameters
     * @return
     */
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        ObjectUtil.checkNotNull(command, "command");
        ObjectUtil.checkNotNull(unit, "unit");
        if (initialDelay < 0) {
            throw new IllegalArgumentException(
                    String.format("initialDelay: %d (expected: >= 0)", initialDelay));
        }
        if (period <= 0) {
            throw new IllegalArgumentException(
                    String.format("period: %d (expected: > 0)", period));
        }
        validateScheduled0(initialDelay, unit);
        validateScheduled0(period, unit);
        //在这里提交定时任务致任务队列
        return schedule(new ScheduledFutureTask<Void>(
                this, Executors.<Void>callable(command, null),
                ScheduledFutureTask.deadlineNanos(unit.toNanos(initialDelay)), unit.toNanos(period)));
    }


    /**
     * 方法会按照指定的固定延迟执行任务。
     * 它会在每次任务执行完成后，等待指定的延迟时间，然后再次执行下一个任务。
     * 与  scheduleAtFixedRate  不同的是， scheduleWithFixedDelay  会确保两次任务之间的时间间隔是固定的。
     * 即使任务的执行时间超过了指定的延迟时间，下一个任务也会等待上一个任务完成后才开始执行。
     *
     * @param command      the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay        the delay between the termination of one
     *                     execution and the commencement of the next
     * @param unit         the time unit of the initialDelay and delay parameters
     * @return
     */
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        ObjectUtil.checkNotNull(command, "command");
        ObjectUtil.checkNotNull(unit, "unit");
        if (initialDelay < 0) {
            throw new IllegalArgumentException(
                    String.format("initialDelay: %d (expected: >= 0)", initialDelay));
        }
        if (delay <= 0) {
            throw new IllegalArgumentException(
                    String.format("delay: %d (expected: > 0)", delay));
        }

        validateScheduled0(initialDelay, unit);
        validateScheduled0(delay, unit);

        return schedule(new ScheduledFutureTask<Void>(
                this, Executors.<Void>callable(command, null),
                ScheduledFutureTask.deadlineNanos(unit.toNanos(initialDelay)), -unit.toNanos(delay)));
    }


    /**
     * 向定时任务队列中添加任务
     *
     * @param task
     * @param <V>
     * @return
     */
    <V> ScheduledFuture<V> schedule(final ScheduledFutureTask<V> task) {
        if (inEventLoop(Thread.currentThread())) {
            scheduledTaskQueue().add(task);
        } else {
            execute(() -> scheduledTaskQueue().add(task));
        }
        return task;
    }


    /**
     * 向定时任务队列中添加任务
     *
     * @param task
     */
    final void removeScheduled(final ScheduledFutureTask<?> task) {
        if (inEventLoop(Thread.currentThread())) {
            scheduledTaskQueue().removeTyped(task);
        } else {
            execute(() -> removeScheduled(task));
        }
    }

    @SuppressWarnings("deprecation")
    private void validateScheduled0(long amount, TimeUnit unit) {
        validateScheduled(amount, unit);
    }


    @Deprecated
    protected void validateScheduled(long amount, TimeUnit unit) {
        // NOOP
    }
}
