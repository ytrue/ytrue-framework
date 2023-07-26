package com.ytrue.netty.channel;

import com.ytrue.netty.util.concurrent.DefaultPromise;
import com.ytrue.netty.util.concurrent.EventExecutor;
import com.ytrue.netty.util.concurrent.Future;
import com.ytrue.netty.util.concurrent.GenericFutureListener;

import static com.ytrue.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * @author ytrue
 * @date 2023-07-26 9:23
 * @description 默认的实现类，服务端和客户端中得到的channelFuture，实际上就是该实现类。
 */
public class DefaultChannelPromise extends DefaultPromise<Void> implements ChannelPromise {

    private final Channel channel;

    public DefaultChannelPromise(Channel channel) {
        this.channel = checkNotNull(channel, "channel");
    }

    public DefaultChannelPromise(Channel channel, EventExecutor executor) {
        super(executor);
        this.channel = checkNotNull(channel, "channel");
    }


    @Override
    protected EventExecutor executor() {
        EventExecutor e = super.executor();
        if (e == null) {
            return channel().eventLoop();
        } else {
            return e;
        }
    }

    @Override
    public Channel channel() {
        return this.channel;
    }


    @Override
    public ChannelPromise setSuccess(Void result) {
        super.setSuccess(result);
        return this;
    }


    @Override
    public ChannelPromise setFailure(Throwable cause) {
        super.setFailure(cause);
        return this;
    }

    @Override
    public ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public ChannelPromise addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
        super.addListeners(listeners);
        return this;
    }

    @Override
    public ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
        super.removeListener(listener);
        return this;
    }

    @Override
    public ChannelPromise removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
        super.removeListeners(listeners);
        return this;
    }

    @Override
    public ChannelPromise sync() throws InterruptedException {
        super.sync();
        return this;
    }

    @Override
    public ChannelPromise syncUninterruptibly() {
        super.syncUninterruptibly();
        return this;
    }

    @Override
    public ChannelPromise await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public ChannelPromise awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }


    @Override
    public boolean isVoid() {
        return false;
    }


    /**
     * 这个方法和下面的方法是本接口中定义的
     *
     * @return
     */
    @Override
    public boolean trySuccess() {
        return trySuccess(null);
    }

    @Override
    public ChannelPromise setSuccess() {
        return setSuccess(null);
    }

    @Override
    public ChannelPromise unvoid() {
        return this;
    }
}
