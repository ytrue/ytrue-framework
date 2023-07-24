package com.ytrue.netty.bootstrap;

import com.ytrue.netty.channel.EventLoopGroup;
import com.ytrue.netty.channel.nio.NioEventLoop;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

/**
 * @author ytrue
 * @date 2023-07-22 14:43
 * @description Bootstrap
 */
@Slf4j
@NoArgsConstructor
public class Bootstrap {

    private NioEventLoop nioEventLoop;

    private SocketChannel socketChannel;

    private EventLoopGroup workerGroup;

    public Bootstrap group(EventLoopGroup childGroup) {
        this.workerGroup = childGroup;
        return this;
    }

    public Bootstrap socketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        return this;
    }

    public void connect(String inetHost, int inetPort) {
        connect(new InetSocketAddress(inetHost, inetPort));
    }


    public void connect(SocketAddress localAddress) {
        doConnect(localAddress);
    }

    private void doConnect(SocketAddress localAddress) {
        //获得单线程执行器
        nioEventLoop = (NioEventLoop) workerGroup.next().next();
        nioEventLoop.setSocketChannel(socketChannel);
        //注册任务先提交
        nioEventLoop.register(socketChannel, nioEventLoop);
        //然后再提交连接服务器任务
        doConnect0(localAddress);
    }

    private void doConnect0(SocketAddress localAddress) {
        nioEventLoop.execute(() -> {
            try {
                socketChannel.connect(localAddress);
                log.info("客户端channel连接服务器成功了");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
    }
}
