package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-25 9:11
 * @description 它是GenericFutureListener的子类，支持进度表示和支持泛型的Future 监听器（有些场景需要多个步骤实现，类似于进度条那样）。
 */
public interface GenericProgressiveFutureListener<F extends ProgressiveFuture<?>> extends GenericFutureListener<F> {

    void operationProgressed(F future, long progress, long total) throws Exception;
}
