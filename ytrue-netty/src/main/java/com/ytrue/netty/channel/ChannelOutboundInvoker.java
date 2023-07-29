package com.ytrue.netty.channel;

import java.net.SocketAddress;

/**
 * @author ytrue
 * @date 2023-07-27 9:09
 * @description 这个接口定义了channel出站时候的方法，这样到channelHandler的时候才明白，但这里要提前引入一下
 * 到时候我会再讲这个的。这里多说一句，在netty中，很多接口定义了一些同名方法，这只是为了让某个类可以调用，但真正干活的
 * 是另一个类中的同名方法。就像NioEventLoopGroup和NioEventLoop那样，一个负责管理，一个负责真正执行
 * 还有一点就是netty中的方法名字起得都很好，见名知意，不认识的名字直接翻译一下就会明白该方法是用来做什么的，这样就不用我一个个解释了
 */
public interface ChannelOutboundInvoker {

    /**
     * 将Channel绑定到指定的本地地址
     *
     * @param localAddress
     * @param promise
     * @return
     */
    ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise);


    /**
     * 将Channel绑定到指定的本地地址
     *
     * @param localAddress
     * @return
     */
    ChannelFuture bind(SocketAddress localAddress);


    /**
     * 连接到远程地址和端口。
     *
     * @param remoteAddress
     * @param promise
     * @return
     */
    ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise);

    /**
     * 连接到远程地址和端口。
     *
     * @param remoteAddress
     * @param localAddress
     * @param promise
     * @return
     */
    ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);


    /**
     * 连接到远程地址和端口
     *
     * @param remoteAddress
     * @return
     */
    ChannelFuture connect(SocketAddress remoteAddress);

    /**
     * 连接到远程地址和端口
     *
     * @param remoteAddress
     * @param localAddress
     * @return
     */
    ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress);

    /**
     * 断开与远程地址的连接。
     *
     * @return
     */
    ChannelFuture disconnect();

    /**
     * 断开与远程地址的连接。
     *
     * @param promise
     * @return
     */
    ChannelFuture disconnect(ChannelPromise promise);

    /**
     * 关闭channel
     *
     * @return
     */
    ChannelFuture close();

    /**
     * 将指定的消息写入底层的发送缓冲区
     *
     * @param msg
     * @return
     */
    ChannelFuture write(Object msg);

    /**
     * 将指定的消息写入底层的发送缓冲区
     *
     * @param msg
     * @param promise
     * @return
     */
    ChannelFuture write(Object msg, ChannelPromise promise);

    /**
     * 刷新底层的发送缓冲区，将缓冲区中的数据立即发送给远程对等端
     *
     * @return
     */
    ChannelOutboundInvoker flush();

    /**
     * 写入数据到通道，并立即刷新通道。
     *
     * @param msg
     * @param promise
     * @return
     */
    ChannelFuture writeAndFlush(Object msg, ChannelPromise promise);

    /**
     * 将指定的消息写入底层的发送缓冲区，并立即刷新缓冲区，确保消息被立即发送。
     *
     * @param msg
     * @return
     */
    ChannelFuture writeAndFlush(Object msg);


    /**
     * 从远程对等端读取数据的
     *
     * @return
     */
    ChannelOutboundInvoker read();


    ChannelFuture close(ChannelPromise promise);


    /**
     * 取消注册通道
     *
     * @return
     */
    ChannelFuture deregister();

    /**
     * 取消注册通道
     *
     * @param promise`
     * @return
     */
    ChannelFuture deregister(ChannelPromise promise);


    /**
     * 创建一个新的 ChannelPromise 对象
     *
     * @return
     */
    ChannelPromise newPromise();

    /**
     * 常见成功的ChannelFuture
     * @return
     */
    ChannelFuture newSucceededFuture();

    /**
     * 创建失败的ChannelFuture
     * @param cause
     * @return
     */
    ChannelFuture newFailedFuture(Throwable cause);
}
