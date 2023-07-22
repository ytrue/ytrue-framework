package com.ytrue.netty.bootstrap;

import com.ytrue.netty.channel.nio.NioEventLoop;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;

/**
 * @author ytrue
 * @date 2023-07-22 14:46
 * @description ServerBootstrap
 */
@Slf4j
@NoArgsConstructor
public class ServerBootstrap {

    private NioEventLoop nioEventLoop;

    private ServerSocketChannel serverSocketChannel;


    public ServerBootstrap nioEventLoop(NioEventLoop nioEventLoop) {
        this.nioEventLoop = nioEventLoop;
        return this;
    }

    public ServerBootstrap serverSocketChannel(ServerSocketChannel serverSocketChannel) {
        this.serverSocketChannel = serverSocketChannel;
        return this;
    }

    public void bind(String host, int inetPort) {
        bind(new InetSocketAddress(host, inetPort));
    }

    public void bind(SocketAddress localAddress) {
        doBind(localAddress);
    }

    private void doBind(SocketAddress localAddress) {
        nioEventLoop.register(serverSocketChannel, this.nioEventLoop);
        doBind0(localAddress);
    }

    /**
     * 这里把绑定端口号封装成一个runnable，提交到单线程执行器的任务队列，绑定端口号仍然由单线程执行器完成
     * 这时候执行器的线程已经启动了
     *
     * @param localAddress
     */
    private void doBind0(SocketAddress localAddress) {
        nioEventLoop.execute(() -> {
            try {
                serverSocketChannel.bind(localAddress);
                log.info("服务端channel和端口号绑定了");
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        });
    }

}
