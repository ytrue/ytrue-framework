package com.ytrue.netty.test;

import com.ytrue.netty.bootstrap.ServerBootstrap;
import com.ytrue.netty.channel.ChannelFuture;
import com.ytrue.netty.channel.ChannelOption;
import com.ytrue.netty.channel.nio.NioEventLoopGroup;
import com.ytrue.netty.channel.socket.nio.NioServerSocketChannel;
import com.ytrue.netty.util.AttributeKey;

import java.io.IOException;

public class ServerTest {

    public static AttributeKey<Integer> INDEX_KEY = AttributeKey.valueOf("常量");

    public static void main(String[] args) throws IOException, InterruptedException {

        ServerBootstrap bootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        ChannelFuture channelFuture = bootstrap.group(bossGroup, workGroup).
                channel(NioServerSocketChannel.class).
                handler(new TestHandlerTwo()).
                option(ChannelOption.SO_BACKLOG, 128).
                childAttr(AttributeKey.valueOf("常量"), 10).
                childHandler(new TestHandlerOne()).
                bind(4444).
                addListener(future -> System.out.println("我绑定成功了")).sync();
    }
}
