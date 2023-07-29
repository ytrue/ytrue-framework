package com.ytrue.netty.channel;

import java.net.SocketAddress;

/**
 * @author ytrue
 * @date 2023/7/29 10:58
 * @description ChannelOutboundHandler
 */
public interface ChannelOutboundHandler extends ChannelHandler {

    /**
     * 绑定本地地址到通道，该方法会触发绑定操作
     *
     * @param ctx
     * @param localAddress
     * @param promise
     * @throws Exception
     */
    void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception;


    /**
     * 连接远程地址，该方法会触发连
     *
     * @param ctx
     * @param remoteAddress
     * @param localAddress
     * @param promise
     * @throws Exception
     */
    void connect(
            ChannelHandlerContext ctx, SocketAddress remoteAddress,
            SocketAddress localAddress, ChannelPromise promise) throws Exception;


    /**
     * 断开与远程节点的连接，该方法会触发断开连接操作
     *
     * @param ctx
     * @param promise
     * @throws Exception
     */
    void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;


    /**
     * 关闭通道，该方法会触发关闭操作
     *
     * @param ctx
     * @param promise
     * @throws Exception
     */
    void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;


    /**
     * 从 EventLoop 中注销通道，该方法会触发注销操作
     *
     * @param ctx
     * @param promise
     * @throws Exception
     */
    void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception;


    /**
     * 通知底层通道读取更多的数据。
     *
     * @param ctx
     * @throws Exception
     */
    void read(ChannelHandlerContext ctx) throws Exception;


    /**
     * 将消息写入通道，该方法会触发写入操作
     *
     * @param ctx
     * @param msg
     * @param promise
     * @throws Exception
     */
    void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception;


    /**
     * 将通道中的所有待发送消息立即刷新到远程节点。
     *
     * @param ctx
     * @throws Exception
     */
    void flush(ChannelHandlerContext ctx) throws Exception;
}
