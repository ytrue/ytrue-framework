package com.ytrue.netty.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * @author ytrue
 * @date 2023-07-25 9:13
 * @description 作为抽象类，定义了get方法的模版让子类使用
 */
public abstract class AbstractFuture<V> implements Future<V> {

    /**
     * 阻塞获取结果
     *
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Override
    public V get() throws InterruptedException, ExecutionException {
        // 阻塞等待
        await();
        // 获取异常
        Throwable cause = cause();
        // 判断是否有异常
        if (cause == null) {
            // 立即获取结果
            return getNow();
        }

        // 判断异常类型，抛出对于的异常
        if (cause instanceof CancellationException) {
            throw (CancellationException) cause;
        }
        throw new ExecutionException(cause);
    }

    /**
     * 阻塞获取结果，指定超时时间
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        //阻塞了用户设定的时间之后
        if (await(timeout, unit)) {
            Throwable cause = cause();
            if (cause == null) {
                return getNow();
            }
            if (cause instanceof CancellationException) {
                throw (CancellationException) cause;
            }
            throw new ExecutionException(cause);
        }
        throw new TimeoutException();
    }
}
