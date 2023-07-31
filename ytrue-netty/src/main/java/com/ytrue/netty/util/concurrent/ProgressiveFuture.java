package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-25 9:11
 * @description 进度条的Future
 */
public interface ProgressiveFuture<V> extends Future<V> {

    @Override
    ProgressiveFuture<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    ProgressiveFuture<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    ProgressiveFuture<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    ProgressiveFuture<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    ProgressiveFuture<V> sync() throws InterruptedException;

    @Override
    ProgressiveFuture<V> syncUninterruptibly();

    @Override
    ProgressiveFuture<V> await() throws InterruptedException;

    @Override
    ProgressiveFuture<V> awaitUninterruptibly();
}
