package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-31 9:30
 * @description DefaultProgressivePromise
 */
public class DefaultProgressivePromise<V> extends DefaultPromise<V> implements ProgressivePromise<V> {

    public DefaultProgressivePromise(EventExecutor executor) {
        super(executor);
    }

    @Override
    public ProgressivePromise<V> setProgress(long progress, long total) {
        if (total < 0) {
            // total unknown
            total = -1; // normalize
            if (progress < 0) {
                throw new IllegalArgumentException("progress: " + progress + " (expected: >= 0)");
            }
        } else if (progress < 0 || progress > total) {
            throw new IllegalArgumentException(
                    "progress: " + progress + " (expected: 0 <= progress <= total (" + total + "))");
        }

        if (isDone()) {
            throw new IllegalStateException("complete already");
        }
        // 暂时忽略，下面同理
        //notifyProgressiveListeners(progress, total);
        return this;
    }

    @Override
    public boolean tryProgress(long progress, long total) {
        if (total < 0) {
            total = -1;
            if (progress < 0 || isDone()) {
                return false;
            }
        } else if (progress < 0 || progress > total || isDone()) {
            return false;
        }

        //notifyProgressiveListeners(progress, total);
        return true;
    }

    @Override
    public ProgressivePromise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public ProgressivePromise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        super.addListeners(listeners);
        return this;
    }

    @Override
    public ProgressivePromise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener) {
        super.removeListener(listener);
        return this;
    }

    @Override
    public ProgressivePromise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        super.removeListeners(listeners);
        return this;
    }

    @Override
    public ProgressivePromise<V> sync() throws InterruptedException {
        super.sync();
        return this;
    }

    @Override
    public ProgressivePromise<V> syncUninterruptibly() {
        super.syncUninterruptibly();
        return this;
    }

    @Override
    public ProgressivePromise<V> await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public ProgressivePromise<V> awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }

    @Override
    public ProgressivePromise<V> setSuccess(V result) {
        super.setSuccess(result);
        return this;
    }

    @Override
    public ProgressivePromise<V> setFailure(Throwable cause) {
        super.setFailure(cause);
        return this;
    }
}
