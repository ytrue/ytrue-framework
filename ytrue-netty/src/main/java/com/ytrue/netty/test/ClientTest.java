package com.ytrue.netty.test;

import com.ytrue.netty.bootstrap.Bootstrap;
import com.ytrue.netty.channel.nio.NioEventLoopGroup;
import com.ytrue.netty.channel.socket.NioSocketChannel;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author ytrue
 * @date 2023-07-22 15:02
 * @description ClientTest
 */
public class ClientTest {

    public static void main(String[] args) throws IOException {

        NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup).
                channel(NioSocketChannel.class);
        bootstrap.connect("127.0.0.1",9999);

    }
}
