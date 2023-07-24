package com.ytrue.netty.test;

import com.ytrue.netty.bootstrap.ServerBootstrap;
import com.ytrue.netty.channel.nio.NioEventLoopGroup;

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
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(2);
        serverBootstrap.group(bossGroup, workerGroup).
                serverSocketChannel(serverSocketChannel);
        serverBootstrap.bind("127.0.0.1", 8080);

    }
}
