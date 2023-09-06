package com.ytrue.gateway.core.socket;

import com.ytrue.gateway.core.session.Configuration;
import com.ytrue.gateway.core.session.defaults.DefaultGatewaySessionFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

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
    private final EventLoopGroup boss = new NioEventLoopGroup(1);

    /**
     * worker
     */
    private final EventLoopGroup work = new NioEventLoopGroup();

    /**
     * netty channel
     */
    private Channel channel;


    /**
     * 配置类
     */
    private DefaultGatewaySessionFactory gatewaySessionFactory;

    public GatewaySocketServer(DefaultGatewaySessionFactory gatewaySessionFactory) {
        this.gatewaySessionFactory = gatewaySessionFactory;
    }

    @Override
    public Channel call() {
        ChannelFuture channelFuture = null;
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, work);
            b.channel(NioServerSocketChannel.class);
            b.option(ChannelOption.SO_BACKLOG, 128);
            b.childHandler(new GatewayChannelInitializer(gatewaySessionFactory));

            // syncUninterruptibly 不会被中断的sync
            channelFuture = b.bind(new InetSocketAddress(7397)).syncUninterruptibly();
            this.channel = channelFuture.channel();

        } catch (Exception e) {
            logger.error("socket server start error.", e);
        } finally {
            if (null != channelFuture && channelFuture.isSuccess()) {
                logger.info("socket server start done.");
            } else {
                logger.error("socket server start error.");
            }
        }
        return channel;
    }
}
