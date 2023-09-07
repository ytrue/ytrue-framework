package com.ytrue.gateway.core.socket;

import com.ytrue.gateway.core.session.Configuration;
import com.ytrue.gateway.core.session.defaults.DefaultGatewaySessionFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * @author ytrue
 * @date 2023-09-06 11:19
 * @description 网关会话服务
 */
public class GatewaySocketServer implements Callable<Channel> {


    private final Logger logger = LoggerFactory.getLogger(GatewaySocketServer.class);

    /**
     * boss
     */
    private EventLoopGroup boss;

    /**
     * worker
     */
    private EventLoopGroup work;

    /**
     * netty channel
     */
    private Channel channel;


    /**
     * 配置类
     */
    private DefaultGatewaySessionFactory gatewaySessionFactory;


    private final Configuration configuration;

    public GatewaySocketServer(Configuration configuration, DefaultGatewaySessionFactory gatewaySessionFactory) {
        this.configuration = configuration;
        this.gatewaySessionFactory = gatewaySessionFactory;
        this.initEventLoopGroup();
    }

    private void initEventLoopGroup() {
        boss = new NioEventLoopGroup(configuration.getBossNThreads());
        work = new NioEventLoopGroup(configuration.getWorkNThreads());
    }


    @Override
    public Channel call() throws InterruptedException {
        ChannelFuture channelFuture = null;

        CountDownLatch countDownLatch = new CountDownLatch(1);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, work);
            b.channel(NioServerSocketChannel.class);
            b.option(ChannelOption.SO_BACKLOG, 128);
            b.childHandler(new GatewayChannelInitializer(configuration, gatewaySessionFactory));
            channelFuture = b.bind(configuration.getPort());
            channelFuture.addListener(future -> {
                if (future.isSuccess()) {
                    logger.info("socket server start done.");
                    ChannelFuture f1 = (ChannelFuture) future;
                    this.channel = f1.channel();
                    countDownLatch.countDown();

                    // 监听关闭
                    logger.info("channel addListener closeFuture start");
                    ChannelFuture closeFuture = channel.closeFuture();
                    closeFuture.addListener(f -> {
                        if (f.isSuccess()) {
                            stopServer();
                        }
                    });
                }

            });
        } catch (Exception e) {
            logger.error("socket server start error.", e);
        }
        countDownLatch.await();
        // 注册非正常关闭
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopServer));
        logger.info("socket server addShutdownHook  success");
        return channel;
    }


    private void stopServer() {
        boss.shutdownGracefully();
        work.shutdownGracefully();
    }
}
