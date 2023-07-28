package com.ytrue.netty.bootstrap;

import com.ytrue.netty.channel.*;
import com.ytrue.netty.util.AttributeKey;
import com.ytrue.netty.util.internal.ObjectUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-07-28 15:34
 * @description Bootstrap
 */
@Slf4j
@NoArgsConstructor
public class Bootstrap extends AbstractBootstrap<Bootstrap, Channel> {

    private volatile ChannelFactory<? extends Channel> channelFactory;

    private final BootstrapConfig config = new BootstrapConfig(this);

    /**
     * 远程地址
     */
    private volatile SocketAddress remoteAddress;


    private Bootstrap(Bootstrap bootstrap) {
        super(bootstrap);
        remoteAddress = bootstrap.remoteAddress;
    }


    public Bootstrap remoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    public ChannelFuture connect(String inetHost, int inetPort) {
        return connect(new InetSocketAddress(inetHost, inetPort));
    }

    public ChannelFuture connect(SocketAddress remoteAddress) {
        ObjectUtil.checkNotNull(remoteAddress, "remoteAddress");
        return doResolveAndConnect(remoteAddress, null);
    }

    private ChannelFuture doResolveAndConnect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
        final ChannelFuture regFuture = initAndRegister();
        //得到要注册的客户端channel
        final Channel channel = regFuture.channel();
        if (regFuture.isDone()) {
            //这里的意思是future执行完成，但是没有成功，那么直接返回future即可
            if (!regFuture.isSuccess()) {
                return regFuture;
            }
            //完成的情况下，直接开始执行绑定端口号的操作,首先创建一个future
            ChannelPromise promise = new DefaultChannelPromise(channel);
            return doResolveAndConnect0(channel, remoteAddress, localAddress, promise);
        } else {
            //该内部类也是在抽象父类中，但这里我又在该类中定义了一遍
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            //给regFuture添加监听器，等regFuture有结果了，监听器开始执行，在监听器中继续执行连接服务端方法
            regFuture.addListener((ChannelFutureListener) future -> {
                Throwable cause = future.cause();
                if (cause != null) {
                    promise.setFailure(cause);
                } else {
                    promise.registered();
                    doResolveAndConnect0(channel, remoteAddress, localAddress, promise);
                }
            });
            return promise;
        }
    }

    private ChannelFuture doResolveAndConnect0(final Channel channel, SocketAddress remoteAddress,
                                               final SocketAddress localAddress, final ChannelPromise promise) {
        try {
            //···
            //前面有一大段解析器解析远程地址的逻辑，在这里我删除了，那些不是重点，我们先关注重点
            doConnect(remoteAddress, localAddress, promise);
        } catch (Throwable cause) {
            promise.tryFailure(cause);
        }
        return promise;
    }

    private static void doConnect(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise connectPromise) {
        //得到客户端的channel
        final Channel channel = connectPromise.channel();
        //仍然是异步注册
        channel.eventLoop().execute(() -> {
            if (localAddress == null) {
                //这里会走这个，我们并没有传递localAddress的地址
                channel.connect(remoteAddress, null, connectPromise);
            }
            //添加该监听器，如果channel连接失败，该监听器会关闭该channel
            connectPromise.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        });
    }


    @Override
    void init(Channel channel) throws Exception {
        final Map<ChannelOption<?>, Object> options = options0();
        synchronized (options) {
            setChannelOptions(channel, options);
        }
        final Map<AttributeKey<?>, Object> attrs = attrs0();
        synchronized (attrs) {
            for (Map.Entry<AttributeKey<?>, Object> e : attrs.entrySet()) {
                channel.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
            }
        }
    }

    @Override
    public Bootstrap validate() {
        super.validate();
//        if (config.handler() == null) {
//            throw new IllegalStateException("handler not set");
//        }
        return this;
    }


    @Override
    public AbstractBootstrapConfig<Bootstrap, Channel> config() {
        return config;
    }

    public SocketAddress remoteAddress() {
        return remoteAddress;
    }
}
