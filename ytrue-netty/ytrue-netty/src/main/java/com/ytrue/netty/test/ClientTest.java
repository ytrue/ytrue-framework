package com.ytrue.netty.test;

import com.ytrue.netty.bootstrap.Bootstrap;
import com.ytrue.netty.channel.nio.NioEventLoop;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author ytrue
 * @date 2023-07-22 15:02
 * @description ClientTest
 */
public class ClientTest {

    public static void main(String[] args) throws IOException {

        SocketChannel socketChannel = SocketChannel.open();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.nioEventLoop(new NioEventLoop(null,socketChannel)).
                socketChannel(socketChannel);
        bootstrap.connect("127.0.0.1",8080);

    }
}
