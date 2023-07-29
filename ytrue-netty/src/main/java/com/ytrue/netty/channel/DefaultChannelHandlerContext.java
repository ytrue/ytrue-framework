package com.ytrue.netty.channel;

import com.ytrue.netty.util.concurrent.EventExecutor;

/**
 * @author ytrue
 * @date 2023/7/29 15:43
 * @description DefaultChannelHandlerContext
 */
public class DefaultChannelHandlerContext extends AbstractChannelHandlerContext {

    private final ChannelHandler handler;

    DefaultChannelHandlerContext(DefaultChannelPipeline pipeline, EventExecutor executor, String name, ChannelHandler handler) {
        super(pipeline, executor, name, handler.getClass());
        this.handler = handler;
    }

    @Override
    public ChannelHandler handler() {
        return handler;
    }
}
