package com.ytrue.netty.test;

import com.ytrue.netty.bootstrap.Bootstrap;
import com.ytrue.netty.channel.ChannelFuture;
import com.ytrue.netty.channel.nio.NioEventLoopGroup;
import com.ytrue.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;

public class ClientTest {
    public static void main(String[] args) throws IOException, InterruptedException {

        NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(workerGroup);

        ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 9993);
    }

}
