package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-31 9:35
 * @description FailedFuture
 */
public class FailedFuture <V> extends CompleteFuture<V> {

    private final Throwable cause;

    public FailedFuture(EventExecutor executor, Throwable cause) {
        super(executor);
        if (cause == null) {
            throw new NullPointerException("cause");
        }
        this.cause = cause;
    }

    @Override
    public Throwable cause() {
        return cause;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public Future<V> sync() {
        //PlatformDependent.throwException(cause);
        return this;
    }

    @Override
    public Future<V> syncUninterruptibly() {
        //PlatformDependent.throwException(cause);
        return this;
    }

    @Override
    public V getNow() {
        return null;
    }
}
