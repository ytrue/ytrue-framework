package com.ytrue.netty.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-07-25 9:07
 * @description netty中重写了该接口，添加了一些重要的方法
 */
public interface Future<V> extends java.util.concurrent.Future<V> {

    /**
     * 是否执行成功
     *
     * @return
     */
    boolean isSuccess();


    /**
     * 标记是否可以通过下面的cancel(boolean mayInterruptIfRunning)取消I/O操作
     *
     * @return
     */
    boolean isCancellable();


    /**
     * 异常
     *
     * @return
     */
    Throwable cause();


    /**
     * 为当前Future实例添加监听Future操作完成的监听器 - isDone()方法激活之后所有监听器实例会得到回调
     *
     * @param listener
     * @return
     */
    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);


    /**
     * 为当前Future实例添加监听Future操作完成的监听器 - isDone()方法激活之后所有监听器实例会得到回调
     *
     * @param listeners
     * @return
     */
    Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);


    /**
     * 为当前Future移除监听Future操作完成的监听器
     *
     * @param listener
     * @return
     */
    Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);


    /**
     * 为当前Future移除监听Future操作完成的监听器
     *
     * @param listeners
     * @return
     */
    Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);


    /**
     * 同步等待Future完成得到最终结果（成功）或者抛出异常（失败），响应中断
     *
     * @return
     * @throws InterruptedException
     */
    Future<V> sync() throws InterruptedException;


    /**
     * 同步等待Future完成得到最终结果（成功）或者抛出异常（失败），不响应中断
     *
     * @return
     */
    Future<V> syncUninterruptibly();


    /**
     * 等待Future完成，响应中断
     *
     * @return
     * @throws InterruptedException
     */
    Future<V> await() throws InterruptedException;


    /**
     * 等待Future完成，不响应中断
     *
     * @return
     */
    Future<V> awaitUninterruptibly();


    /**
     * 带超时时限的等待Future完成，响应中断
     *
     * @param timeout
     * @param unit
     * @return
     * @throws InterruptedException
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;


    /**
     * 带超时时限的等待Future完成，响应中断
     *
     * @param timeoutMillis
     * @return
     * @throws InterruptedException
     */
    boolean await(long timeoutMillis) throws InterruptedException;


    /**
     * 带超时时限的等待Future完成，不响应中断
     *
     * @param timeout
     * @param unit
     * @return
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);


    /**
     * 带超时时限的等待Future完成，不响应中断
     *
     * @param timeoutMillis
     * @return
     */
    boolean awaitUninterruptibly(long timeoutMillis);


    /**
     * 非阻塞马上返回Future的结果，如果Future未完成，此方法一定返回null；有些场景下如果Future成功获取到的结果是null则需要二次检查isDone()方法是否为true
     *
     * @return
     */
    V getNow();

    /**
     * 取消当前Future实例的执行，如果取消成功会抛出CancellationException异常
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     *                              task should be interrupted; otherwise, in-progress tasks are allowed
     *                              to complete
     * @return
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);
}
