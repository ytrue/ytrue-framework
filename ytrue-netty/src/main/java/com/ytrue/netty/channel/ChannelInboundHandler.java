package com.ytrue.netty.channel;

/**
 * @author ytrue
 * @date 2023/7/29 10:58
 * @description ChannelInboundHandler
 */
public interface ChannelInboundHandler extends ChannelHandler {
    /**
     * 当通道被注册到 EventLoop 时调用，可以在此方法中执行一些初始化操作。
     * @param ctx
     * @throws Exception
     */
    void channelRegistered(ChannelHandlerContext ctx) throws Exception;


    /**
     * 当通道从 EventLoop 中注销时调用，可以在此方法中执行一些清理操作。
     * @param ctx
     * @throws Exception
     */
    void channelUnregistered(ChannelHandlerContext ctx) throws Exception;


    /**
     * 当通道变为活跃状态时调用，表示通道已经连接到远程节点。
     * @param ctx
     * @throws Exception
     */
    void channelActive(ChannelHandlerContext ctx) throws Exception;


    /**
     * 当通道变为非活跃状态时调用，表示通道已经断开连接。
     * @param ctx
     * @throws Exception
     */
    void channelInactive(ChannelHandlerContext ctx) throws Exception;


    /**
     * 当从通道读取到数据时调用，可以在此方法中对接收到的数据进行处理。
     * @param ctx
     * @param msg
     * @throws Exception
     */
    void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception;

    /**
     * 当通道上的数据读取完成时调用，可以在此方法中执行一些收尾操作。
     * @param ctx
     * @throws Exception
     */
    void channelReadComplete(ChannelHandlerContext ctx) throws Exception;


    /**
     * 当用户自定义事件被触发时调用，可以在此方法中对自定义事件进行处理。
     * @param ctx
     * @param evt
     * @throws Exception
     */
    void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception;


    /**
     * 当通道的可写状态发生变化时调用，可以在此方法中根据通道的可写状态进行相应的处理。
     * @param ctx
     * @throws Exception
     */
    void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception;


    /**
     * 处理过程中发生异常时调用，可以在此方法中对异常进行处理。
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    @SuppressWarnings("deprecation")
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
}
