package com.ytrue.netty.util.concurrent;

import java.util.Iterator;
import java.util.concurrent.*;

/**
 * @author ytrue
 * @date 2023-07-24 9:04
 * @description 循环组的接口, 暂时先不继承ScheduledExecutorService接口了
 */
public interface EventExecutorGroup extends ScheduledExecutorService, Iterable<EventExecutor> {

    /**
     * 获取一个EventExecutor
     *
     * @return
     */
    EventExecutor next();

    /**
     * 优雅停机
     */
    void shutdownGracefully();


    @Override
    Iterator<EventExecutor> iterator();


    /**
     *  提交一个Runnable任务进行执行，并返回一个Future对象。
     * @param task the task to submit
     * @return
     */
    @Override
    Future<?> submit(Runnable task);

    /**
     *  提交一个Runnable任务进行执行，并返回一个Future对象。
     * @param task the task to submit
     * @param result the result to return
     * @return
     * @param <T>
     */
    @Override
    <T> Future<T> submit(Runnable task, T result);

    /**
     *  提交一个Runnable任务进行执行，并返回一个Future对象。
     * @param task the task to submit
     * @return
     * @param <T>
     */
    @Override
    <T> Future<T> submit(Callable<T> task);

    /**
     * 在指定的延迟时间后执行给定的任务。
     * @param command the task to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @return
     */
    @Override
    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    /**
     * 在指定的延迟时间后执行给定的任务。
     * @param callable the function to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     * @return
     * @param <V>
     */
    @Override
    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    /**
     * 在给定的初始延迟时间后开始执行任务，并以固定的延迟时间间隔执行任务。
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit the time unit of the initialDelay and period parameters
     * @return
     */
    @Override
    ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    /**
     * 在给定的初始延迟时间后开始执行任务，并以固定的时间间隔周期性地执行任务。
     *
     * @param command      the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay        the delay between the termination of one
     *                     execution and the commencement of the next
     * @param unit         the time unit of the initialDelay and delay parameters
     * @return
     */
    @Override
    ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);
}
