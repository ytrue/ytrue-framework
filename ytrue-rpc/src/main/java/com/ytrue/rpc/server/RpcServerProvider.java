package com.ytrue.rpc.server;

import com.ytrue.rpc.register.HostAndPort;
import com.ytrue.rpc.register.Registry;
import com.ytrue.rpc.utils.NetUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

/**
 * @author ytrue
 * @date 2023-05-19 12:54
 * @description RpcServerProvider
 */
public class RpcServerProvider {

    /**
     * 端口号
     */
    private final int port;

    /**
     * boos
     */
    private final EventLoopGroup eventLoopGroupBoss;

    /**
     * work
     */
    private final EventLoopGroup eventLoopGroupWorker;

    /**
     * Netty的编解码器 内置Handler通过这个线程组服务
     */
    private final EventLoopGroup eventLoopGroupHandler;

    /**
     * service
     */
    private final EventLoopGroup eventLoopGroupService;

    /**
     * register
     */
    private final Registry registry;

    /**
     * serverBootstrap
     */
    private final ServerBootstrap serverBootstrap;

    /**
     * key = 服务的接口, value = 服务对象
     */
    private final Map<String, Object> exposeBeans;

    /**
     * 启动状态
     */
    private boolean isStarted;


    public RpcServerProvider(Registry registry, Map<String, Object> exposeBeans) {
        this(NetUtil.getUsablePort(5001), 1, 1, 1, registry, exposeBeans);
    }

    public RpcServerProvider(int port, Registry registry, Map<String, Object> exposeBeans) {
        this(port, 1, 1, 1, registry, exposeBeans);
    }


    public RpcServerProvider(int port, int workerThreads, int handlerThreads, int serviceThreads, Registry registry, Map<String, Object> exposeBeans) {
        // 设置端口
        this.port = port;

        // 线程组
        this.eventLoopGroupBoss = new NioEventLoopGroup(1);
        this.eventLoopGroupWorker = new NioEventLoopGroup(workerThreads);
        this.eventLoopGroupHandler = new DefaultEventLoopGroup(handlerThreads);
        this.eventLoopGroupService = new DefaultEventLoopGroup(serviceThreads);

        // 注册器
        this.registry = registry;
        // 关系衍射
        this.exposeBeans = exposeBeans;
        // netty server
        this.serverBootstrap = new ServerBootstrap();
    }


    /**
     * 启动服务
     */
    public void startServer() {
        // 校验，防止多次启动，出现端口占用
        if (isStarted) {
            throw new RuntimeException("server is already started....");
        }

        serverBootstrap.channel(NioServerSocketChannel.class);
        serverBootstrap.group(eventLoopGroupBoss, eventLoopGroupWorker);
        serverBootstrap.childHandler(new RpcServerProviderInitializer(eventLoopGroupHandler, eventLoopGroupService, exposeBeans));

        final ChannelFuture channelFuture = serverBootstrap.bind(port);
        // GenericFutureListener
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                //2服务注册功能
                registerServices(NetUtil.getHost(), port, exposeBeans, registry);
                isStarted = true;

                //监听关闭
                Channel channel = channelFuture.channel();
                ChannelFuture closeFuture = channel.closeFuture();
                // GenericFutureListener
                closeFuture.addListener(f -> {
                    if (f.isSuccess()) {
                        stopServer();
                    }
                });
            }
        });

        // 注册非正常关闭
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopServer));
    }

    /**
     * 关闭服务 同时释放资源
     */
    private void stopServer() {
        eventLoopGroupBoss.shutdownGracefully();
        eventLoopGroupWorker.shutdownGracefully();
        eventLoopGroupHandler.shutdownGracefully();
        eventLoopGroupService.shutdownGracefully();
    }

    /**
     * 服务注册
     *
     * @param hostAddress
     * @param port
     * @param exposeBeans
     * @param registry
     */
    private void registerServices(String hostAddress, int port, Map<String, Object> exposeBeans, Registry registry) {
        //1 通过exposeBeans中获得所有的服务的对象
        Set<String> keySet = exposeBeans.keySet();

        //2 遍历这些对象通过registry进行注册
        HostAndPort hostAndPort = new HostAndPort(hostAddress, port);
        for (String targetInterface : keySet) {
            registry.registerService(targetInterface, hostAndPort);
        }
    }


}
