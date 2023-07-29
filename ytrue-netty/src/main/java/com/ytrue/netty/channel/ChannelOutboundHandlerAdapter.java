package com.ytrue.netty.channel;

import java.net.SocketAddress;
import com.ytrue.netty.channel.ChannelHandlerMask.Skip;

/**
 * @author ytrue
 * @date 2023/7/29 10:58
 * @description ChannelOutboundHandlerAdapter
 */
public class ChannelOutboundHandlerAdapter extends ChannelHandlerAdapter implements ChannelOutboundHandler {
    @Skip
    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress,
                     ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }


    @Skip
    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                        SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
    }


    @Skip
    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
            throws Exception {
        ctx.disconnect(promise);
    }


    @Skip
    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise)
            throws Exception {
        ctx.close(promise);
    }


    @Skip
    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);
    }


    @Skip
    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }


    @Skip
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
    }


    @Skip
    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
