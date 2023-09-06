package com.ytrue.gateway.core.socket;

import com.ytrue.gateway.core.session.defaults.DefaultGatewaySessionFactory;
import com.ytrue.gateway.core.socket.handlers.GatewayServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * @author ytrue
 * @date 2023-09-06 11:24
 * @description SessionChannelInitializer
 */
public class GatewayChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final DefaultGatewaySessionFactory gatewaySessionFactory;

    public GatewayChannelInitializer(DefaultGatewaySessionFactory gatewaySessionFactory) {
        this.gatewaySessionFactory = gatewaySessionFactory;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline line = channel.pipeline();

       /* line.addLast(new HttpRequestDecoder());
        line.addLast(new HttpResponseEncoder());*/

        line.addLast(new HttpServerCodec());
        line.addLast(new HttpObjectAggregator(1024 * 1024));
        line.addLast(new GatewayServerHandler(gatewaySessionFactory));

    }
}
