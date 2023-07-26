package com.ytrue.netty.bootstrap;

import com.ytrue.netty.channel.*;
import com.ytrue.netty.channel.nio.NioEventLoop;
import com.ytrue.netty.util.concurrent.EventExecutor;
import com.ytrue.netty.util.internal.ObjectUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author ytrue
 * @date 2023-07-22 14:43
 * @description Bootstrap
 */
@Slf4j
@NoArgsConstructor
public class Bootstrap<C extends Channel> {

    private NioEventLoop nioEventLoop;

    private EventLoopGroup workerGroup;

    private volatile ChannelFactory<? extends Channel> channelFactory;


    public Bootstrap group(EventLoopGroup childGroup) {
        this.workerGroup = childGroup;
        return this;
    }

    public Bootstrap channel(Class<? extends C> channelClass) {
        this.channelFactory = new ReflectiveChannelFactory<C>(channelClass);
        return this;
    }

    public void connect(String inetHost, int inetPort) {
        connect(new InetSocketAddress(inetHost, inetPort));
    }


    public ChannelFuture connect(SocketAddress remoteAddress) {
        ObjectUtil.checkNotNull(remoteAddress, "remoteAddress");
        return doResolveAndConnect(remoteAddress, null);
    }

    private ChannelFuture doResolveAndConnect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
        // 这里的逻辑和serverbootstarp一样，但是在这里又要写一遍该方法，现在是不是发现，如果bootstarp和serverbootstarp有一个
        // 抽象父类就好了，就可以在父类中定义模版方法了。实际上源码中确实有一个父类，这个方法被定义在父类中，但我们暂时还不引入
        // 整个方法和serverbootstrap中的doBind方法类似，判断和处理逻辑几乎一样

        final ChannelFuture regFuture = initAndRegister();
        //得到要注册的kehuduanchannel
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
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        promise.setFailure(cause);
                    } else {
                        promise.registered();
                        doResolveAndConnect0(channel, remoteAddress, localAddress, promise);
                    }
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

    private static void doConnect(
            final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise connectPromise) {
        //得到客户端的channel
        final Channel channel = connectPromise.channel();
        //仍然是异步注册
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                if (localAddress == null) {
                    //这里会走这个，我们并没有传递localAddress的地址
                    channel.connect(remoteAddress,null, connectPromise);
                }
                //添加该监听器，如果channel连接失败，该监听器会关闭该channel
                connectPromise.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        });
    }

    final ChannelFuture initAndRegister() {
        Channel channel;
        //在这里初始化服务端channel，反射创建对象调用的无参构造器，
        //可以去NioServerSocketChannel类中看看无参构造器中做了什么
        channel = channelFactory.newChannel();
        //这里是异步注册的，一般来说，workerGroup设置的也是一个线程执行器。只有在服务端的workerGroup中，才会设置多个线程执行器
        ChannelFuture regFuture = workerGroup.next().register(channel);
        return regFuture;
    }


    static final class PendingRegistrationPromise extends DefaultChannelPromise {

        private volatile boolean registered;

        PendingRegistrationPromise(Channel channel) {
            super(channel);
        }

        //该方法是该静态类独有的，该方法被调用的时候，registered赋值为true
        void registered() {
            registered = true;
        }

        /**
         * 该方法简化一下， 全局的执行器不是必须引入的
         *
         * @return
         */
        @Override
        protected EventExecutor executor() {
            return super.executor();

        }
    }

}
