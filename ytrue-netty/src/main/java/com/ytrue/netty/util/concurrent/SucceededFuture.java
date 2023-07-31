package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-31 9:35
 * @description SucceededFuture
 */
public class SucceededFuture <V> extends CompleteFuture<V> {
    private final V result;


    public SucceededFuture(EventExecutor executor, V result) {
        super(executor);
        this.result = result;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public V getNow() {
        return result;
    }
}
