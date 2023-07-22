package com.ytrue.netty.test;

import com.ytrue.netty.bootstrap.ServerBootstrap;
import com.ytrue.netty.channel.nio.NioEventLoop;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

/**
 * @author ytrue
 * @date 2023-07-22 14:53
 * @description ServerTest
 */
public class ServerTest {
    public static void main(String[] args) throws IOException {

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        NioEventLoop boss = new NioEventLoop(serverSocketChannel, null);
        NioEventLoop worker = new NioEventLoop(serverSocketChannel, null);

        //把worker执行器设置到boss执行器中，这样在boss执行器中接收到客户端连接，可以立刻提交给worker执行器
        boss.setWorker(worker);

        serverBootstrap.nioEventLoop(boss).
                serverSocketChannel(serverSocketChannel);
        serverBootstrap.bind("127.0.0.1",8080);

    }
}
