package com.ytrue.rpc.transport;

import com.ytrue.rpc.future.SyncWrite;
import com.ytrue.rpc.protocol.RpcRequest;
import com.ytrue.rpc.protocol.RpcResponse;
import com.ytrue.rpc.register.HostAndPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ytrue
 * @date 2023-05-19 19:42
 * @description NettyTransport
 */
@Slf4j
public class NettyTransport implements Transport {

    private Bootstrap bootstrap;

    private EventLoopGroup worker;

    public NettyTransport() {
        this(1);
    }

    public NettyTransport(int workerThreads) {
        bootstrap = new Bootstrap();
        worker = new NioEventLoopGroup(workerThreads);
        bootstrap.group(worker);
        bootstrap.channel(NioSocketChannel.class);
    }

    @Override
    public RpcResponse invoke(HostAndPort hostAndPort, RpcRequest request) throws Exception {

        RpcClientChannelInitializer rpcClientChannelInitializer = new RpcClientChannelInitializer(request);
        bootstrap.handler(rpcClientChannelInitializer);

        ChannelFuture channelFuture = bootstrap.connect(hostAndPort.getHostName(), hostAndPort.getPort()).sync();
        // 发送数据
        return new SyncWrite().writeAndSync(channelFuture.channel(), request, Integer.MAX_VALUE);
    }

    @Override
    public void close() {
        worker.shutdownGracefully();
    }
}
