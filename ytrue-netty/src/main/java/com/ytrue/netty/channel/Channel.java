package com.ytrue.netty.channel;

import com.ytrue.netty.util.AttributeMap;

import java.net.SocketAddress;

/**
 * @author ytrue
 * @date 2023-07-26 9:07
 * @description Channel
 */
public interface Channel extends AttributeMap, ChannelOutboundInvoker {

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
     * 从远程对等端读取数据的
     *
     * @return
     */
    @Override
    Channel read();

    /**
     * 刷新底层的发送缓冲区，将缓冲区中的数据立即发送给远程对等端
     *
     * @return
     */
    @Override
    Channel flush();


    /**
     * 终于引入了Unsafe类
     *
     * @return
     */
    Unsafe unsafe();

    ChannelPipeline pipeline();

    interface Unsafe {
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
         * channel 注册 到select
         *
         * @param eventLoop
         * @param promise
         */
        void register(EventLoop eventLoop, ChannelPromise promise);


        /**
         * 将Channel绑定到指定的本地地址
         *
         * @param localAddress
         * @param promise
         */
        void bind(SocketAddress localAddress, ChannelPromise promise);

        /**
         * 使用指定的本地和远程地址连接到远程对等端
         *
         * @param remoteAddress
         * @param localAddress
         * @param promise
         */
        void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise);

        /**
         * 断开与远程对等端的连接
         *
         * @param promise
         */
        void disconnect(ChannelPromise promise);

        /**
         * 关闭Channel
         *
         * @param promise
         */
        void close(ChannelPromise promise);

        /**
         * 用于强制关闭Channel。它的作用是立即关闭Channel，并丢弃所有未发送的数据。与普通的close()方法不同，
         * closeForcibly()方法不会等待未发送的数据被发送完毕，而是立即关闭连接
         * closeForcibly()方法在某些情况下可能会导致数据的丢失，因为未发送的数据将被丢弃而不会被发送。因此，应该谨慎使用closeForcibly()方法，并确保在关闭Channel之前没有任何重要的未发送数据。
         * 一般情况下，推荐使用close()方法来正常关闭Channel
         * ，以确保数据的可靠传输。只有在必要的情况下，
         * 才应该使用closeForcibly()方法来强制关闭Channel。
         */
        void closeForcibly();

        /**
         * 取消Channel的注册
         *
         * @param promise
         */
        void deregister(ChannelPromise promise);

        /**
         * 开始从Channel中读取数据，并将读取到的数据传递给ChannelPipeline中的下一个ChannelInboundHandler进行处理。
         */
        void beginRead();

        /**
         * 将指定的消息写入底层的发送缓冲区
         *
         * @param msg
         * @param promise
         */
        void write(Object msg, ChannelPromise promise);

        /**
         * 刷新底层的发送缓冲区，将缓冲区中的数据立即发送给远程对等端。
         */
        void flush();


        ChannelPromise voidPromise();
    }
}
