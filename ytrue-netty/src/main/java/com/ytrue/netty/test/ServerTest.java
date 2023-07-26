package com.ytrue.netty.test;

import com.ytrue.netty.bootstrap.ServerBootstrap;
import com.ytrue.netty.channel.ChannelFuture;
import com.ytrue.netty.channel.nio.NioEventLoopGroup;
import com.ytrue.netty.channel.socket.NioServerSocketChannel;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

/**
 * @author ytrue
 * @date 2023-07-22 14:53
 * @description ServerTest
 */
public class ServerTest {
    public static void main(String[] args) throws IOException {

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(2);
        ChannelFuture channelFuture = serverBootstrap.
                group(bossGroup,workerGroup).
                channel(NioServerSocketChannel.class).
                bind(9999).addListener(future -> System.out.println("我绑定成功了"));

    }
}
