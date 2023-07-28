package com.ytrue.netty.test;

import com.ytrue.netty.bootstrap.Bootstrap;
import com.ytrue.netty.channel.nio.NioEventLoopGroup;
import com.ytrue.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Test {
    public static void main(String[] args) throws InterruptedException {

        for (int i = 0; i < 300; i++) {
            int finalI = i;
            new Thread(() -> {
                NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(workerGroup).
                        channel(NioSocketChannel.class);

                bootstrap.connect("127.0.0.1", 8080);
                log.info("我是：{}", String.valueOf(finalI));
            }).start();
        }


        Thread.currentThread().join();


//        NioEventLoopGroup workerGroup = new NioEventLoopGroup(1);
//        Bootstrap bootstrap = new Bootstrap();
//        bootstrap.group(workerGroup).
//                channel(NioSocketChannel.class);
//
//        NioEventLoopGroup workerGroup1 = new NioEventLoopGroup(1);
//        Bootstrap bootstrap1 = new Bootstrap();
//        bootstrap1.group(workerGroup1).
//                channel(NioSocketChannel.class);
//
//        NioEventLoopGroup workerGroup2 = new NioEventLoopGroup(1);
//        Bootstrap bootstrap2 = new Bootstrap();
//        bootstrap2.group(workerGroup2).
//                channel(NioSocketChannel.class);
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                bootstrap.connect("127.0.0.1", 8080);
//            }
//        }).start();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                bootstrap1.connect("127.0.0.1", 8080);
//            }
//        }).start();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                bootstrap2.connect("127.0.0.1", 8080);
//            }
//        }).start();
    }


}
