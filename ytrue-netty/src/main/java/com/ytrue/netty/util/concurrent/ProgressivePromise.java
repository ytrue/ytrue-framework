package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-31 9:31
 * @description ProgressivePromise
 */
public interface ProgressivePromise <V> extends Promise<V>, ProgressiveFuture<V> {

    ProgressivePromise<V> setProgress(long progress, long total);

    boolean tryProgress(long progress, long total);

    @Override
    ProgressivePromise<V> setSuccess(V result);

    @Override
    ProgressivePromise<V> setFailure(Throwable cause);

    @Override
    ProgressivePromise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    ProgressivePromise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    ProgressivePromise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    ProgressivePromise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    ProgressivePromise<V> await() throws InterruptedException;

    @Override
    ProgressivePromise<V> awaitUninterruptibly();

    @Override
    ProgressivePromise<V> sync() throws InterruptedException;

    @Override
    ProgressivePromise<V> syncUninterruptibly();
}
