package com.ytrue.gateway.core.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author ytrue
 * @date 2023-09-06 11:19
 * @description BaseHandler
 */
public abstract class BaseHandler<T> extends SimpleChannelInboundHandler<T> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, T msg) throws Exception {
        session(ctx, ctx.channel(), msg);
    }

    /**
     * 处理会话
     *
     * @param ctx
     * @param channel
     * @param request
     */
    protected abstract void session(ChannelHandlerContext ctx, final Channel channel, T request);

}
