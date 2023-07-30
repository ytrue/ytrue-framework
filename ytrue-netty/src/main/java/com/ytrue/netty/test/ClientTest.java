package com.ytrue.netty.test;

import com.ytrue.netty.bootstrap.Bootstrap;
import com.ytrue.netty.channel.Channel;
import com.ytrue.netty.channel.ChannelFuture;
import com.ytrue.netty.channel.DefaultChannelPromise;
import com.ytrue.netty.channel.nio.NioEventLoopGroup;
import com.ytrue.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ClientTest {
    public static void main(String[] args) throws IOException, InterruptedException {

        NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        ChannelFuture channelFuture = bootstrap.group(workerGroup).
                channel(NioSocketChannel.class).
                handler(new TestHandlerOne()).
                connect("127.0.0.1",8080);
        Thread.sleep(4000);
        Channel channel = channelFuture.channel();
        channel.writeAndFlush(ByteBuffer.wrap("我是真正的netty！".getBytes()));
    }

}
