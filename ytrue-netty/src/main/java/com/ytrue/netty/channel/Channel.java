package com.ytrue.netty.channel;

import java.net.SocketAddress;

/**
 * @author ytrue
 * @date 2023-07-26 9:07
 * @description Channel
 */
public interface Channel {

    /**
     * 在客户端连接建立后，生成Channel通道的时候会为每一个Channel分配一个唯一的ID，该ID可能的生成策略有：
     * <p>
     * <p>
     * 机器的MAC地址（EUI-48或者EUI-64）等可以代表全局唯一的信息
     * 当前的进程ID
     * 当前系统时间的毫秒
     * 当前系统时间纳秒数
     * 32位的随机整型数
     * 32位自增的序列数
     *
     * @return
     */
    ChannelId id();


    /**
     * 在上面说过Channel建立后会与EventLoopGroop中分配的一个EventLoop线程绑定，
     * 该方法就可以获取到Channel绑定的EventLoop。EventLoop本质上就是处理网络I/O读写事件的Reactor线程。
     * 在Netty中，它不仅用来处理网络事件，也可以用来执行定时任务和用户自定义NioTask任务等。
     *
     * @return
     */
    EventLoop eventLoop();


    /**
     * 返回该Channel的父Channel。对于服务端的Channel而言，它的父Channel为空；对于客户端Channel而言，它的父Channel就是创建它的ServerSocketChannel
     *
     * @return
     */
    Channel parent();


    /**
     * 获取当前Channel的配置信息，例如：CONNECT_TIMEOUT_MILLIS
     *
     * @return
     */
    ChannelConfig config();


    /**
     * 判断当前Channel是否已经打开
     *
     * @return
     */
    boolean isOpen();

    /**
     * 判断当Channel是否已经注册到NioEventLoop上
     *
     * @return
     */
    boolean isRegistered();


    /**
     * 判断当前Channel是否已经处于激活状态
     *
     * @return
     */
    boolean isActive();

    /**
     * 获取当前Channel的本地绑定地址
     *
     * @return
     */
    SocketAddress localAddress();


    /**
     * 获取当前Channel通信的远程Socket地址
     *
     * @return
     */
    SocketAddress remoteAddress();


    /**
     * 用于监听Channel的关闭事件。当调用Channel.close()方法关闭Channel时，
     * 会触发关闭事件，并且返回的ChannelFuture对象将在Channel关闭完成后得到通知。
     * 通过对ChannelFuture对象添加监听器，可以在Channel关闭完成后执行相应的操作。
     *
     * @return
     */
    ChannelFuture closeFuture();


    /**
     * @Author: PP-jessica
     * @Description:该方法并不在此接口，而是在ChannelOutboundInvoker接口，现在先放在这里
     */
    ChannelFuture close();

    /**
     * @Author: PP-jessica
     * @Description:该方法并不在此接口，而是在ChannelOutboundInvoker接口，现在先放在这里
     */
    void bind(SocketAddress localAddress, ChannelPromise promise);

    /**
     * @Author: PP-jessica
     * @Description:该方法并不在此接口，而是在ChannelOutboundInvoker接口，现在先放在这里
     */
    void connect(SocketAddress remoteAddress, final SocketAddress localAddress, ChannelPromise promise);

    /**
     * @Author: PP-jessica
     * @Description:该方法并不在此接口，而是在unsafe接口，现在先放在这里
     */
    void register(EventLoop eventLoop, ChannelPromise promise);

    /**
     * @Author: PP-jessica
     * @Description:该方法并不在此接口，而是在unsafe接口，现在先放在这里
     */
    void beginRead();
}
