package com.ytrue.netty.channel;

import com.ytrue.netty.util.concurrent.GenericFutureListener;

/**
 * @author ytrue
 * @date 2023-07-26 9:30
 * @description 和channel有关的监听器
 */
public interface ChannelFutureListener extends GenericFutureListener<ChannelFuture> {

    /**
     * 关闭channel监听器
     */
    ChannelFutureListener CLOSE = future -> future.channel().close();


    /**
     * 如果失败，关闭channel监听器
     */
    ChannelFutureListener CLOSE_ON_FAILURE = future -> {
        if (!future.isSuccess()) {
            future.channel().close();
        }
    };

}
