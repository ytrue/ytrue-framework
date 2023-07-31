package com.ytrue.netty.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-07-31 9:36
 * @description CompleteFuture 完成的Future
 */
public abstract class CompleteFuture<V> extends AbstractFuture<V> {

    private final EventExecutor executor;

    protected CompleteFuture(EventExecutor executor) {
        this.executor = executor;
    }


    protected EventExecutor executor() {
        return executor;
    }

    @Override
    public Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        DefaultPromise.notifyListener(executor(), this, listener);
        return this;
    }

    @Override
    public Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        if (listeners == null) {
            throw new NullPointerException("listeners");
        }
        for (GenericFutureListener<? extends Future<? super V>> l: listeners) {
            if (l == null) {
                break;
            }
            DefaultPromise.notifyListener(executor(), this, l);
        }
        return this;
    }

    @Override
    public Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener) {
        // NOOP
        return this;
    }

    @Override
    public Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        // NOOP
        return this;
    }

    @Override
    public Future<V> await() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return this;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return true;
    }

    @Override
    public Future<V> sync() throws InterruptedException {
        return this;
    }

    @Override
    public Future<V> syncUninterruptibly() {
        return this;
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return true;
    }

    @Override
    public Future<V> awaitUninterruptibly() {
        return this;
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return true;
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        return true;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean isCancellable() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }
}
