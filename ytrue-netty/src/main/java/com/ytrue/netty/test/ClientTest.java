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
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(workerGroup);

        ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 9993).sync();


        /*
         * 人为将程序中止，等待连接创建完成,否则会报如下错误
         * java.nio.channels.NotYetConnectedException at sun.nio.ch.SocketChannelImpl.ensureWriteOpen(SocketChannelImpl.java:274)
         */
        Thread.sleep(3000);

        Channel channel = channelFuture.channel();
        channel.writeAndFlush(ByteBuffer.wrap("我是真正的netty！".getBytes()));

//        channelFuture.addListener(future -> {
//            Thread.sleep(3000);
//            DefaultChannelPromise defaultPromise = (DefaultChannelPromise) future;
//            defaultPromise.channel().writeAndFlush(ByteBuffer.wrap("我是真正的netty！".getBytes()));
//        });


        Thread.currentThread().join();
    }

}
