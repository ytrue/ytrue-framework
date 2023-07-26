package com.ytrue.netty.channel;

import com.ytrue.netty.util.concurrent.Future;
import com.ytrue.netty.util.concurrent.GenericFutureListener;
import com.ytrue.netty.util.concurrent.Promise;

/**
 * @author ytrue
 * @date 2023-07-26 9:15
 * @description ChannelPromise
 */
public interface ChannelPromise extends ChannelFuture, Promise<Void> {

    @Override
    Channel channel();

    @Override
    ChannelPromise setSuccess(Void result);


    @Override
    ChannelPromise setFailure(Throwable cause);

    @Override
    ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelPromise sync() throws InterruptedException;

    @Override
    ChannelPromise syncUninterruptibly();

    @Override
    ChannelPromise await() throws InterruptedException;

    @Override
    ChannelPromise awaitUninterruptibly();

    /**
     * 这个方法和下面的方法是本接口中定义的
     *
     * @return
     */
    ChannelPromise setSuccess();

    boolean trySuccess();

    ChannelPromise unvoid();
}
