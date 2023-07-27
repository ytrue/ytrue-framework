package com.ytrue.netty.bootstrap;

import com.ytrue.netty.channel.*;
import com.ytrue.netty.util.concurrent.EventExecutor;
import com.ytrue.netty.util.internal.ObjectUtil;
import com.ytrue.netty.util.internal.SocketUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author ytrue
 * @date 2023-07-22 14:46
 * @description ServerBootstrap
 */
@Slf4j
@NoArgsConstructor
public class ServerBootstrap<C extends Channel> {

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private volatile ChannelFactory<? extends Channel> channelFactory;


    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        this.bossGroup = parentGroup;
        this.workerGroup = childGroup;
        return this;
    }

    /**
     * @Author: PP-jessica
     * @Description:创建channel的工厂
     */
    public ServerBootstrap channel(Class<? extends C> channelClass) {
        this.channelFactory = new ReflectiveChannelFactory<C>(channelClass);
        return this;
    }

    public ChannelFuture bind(int inetPort) {
        return bind(new InetSocketAddress(inetPort));
    }

    public ChannelFuture bind(String inetHost, int inetPort) {
        return bind(SocketUtils.socketAddress(inetHost, inetPort));
    }

    public ChannelFuture bind(InetAddress inetHost, int inetPort) {
        return bind(new InetSocketAddress(inetHost, inetPort));
    }

    public ChannelFuture bind(SocketAddress localAddress) {
        return doBind(ObjectUtil.checkNotNull(localAddress, "localAddress"));
    }


    private ChannelFuture doBind(SocketAddress localAddress) {
        //服务端的channel在这里初始化，然后注册到单线程执行器的selector上
        final ChannelFuture regFuture = initAndRegister();
        //得到服务端的channel
        Channel channel = regFuture.channel();
        //要判断future有没有完成
        if (regFuture.isDone()) {
            //完成的情况下，直接开始执行绑定端口号的操作,首先创建一个future
            ChannelPromise promise = new DefaultChannelPromise(channel);
            //执行绑定方法
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {
            //走到这里，说明上面的initAndRegister方法中，服务端的channel还没有完全注册到单线程执行器的selector上
            //此时可以直接则向regFuture添加回调函数，这里有个专门的静态内部类，用来协助判断服务端channel是否注册成功
            //该回调函数会在regFuture完成的状态下被调用，在回调函数中进行服务端的绑定，回顾一下第四课就明白了。
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        promise.setFailure(cause);
                    } else {
                        //走到这里说明服务端channel在注册过程中没有发生异常，已经注册成功，可以开始绑定端口号了
                        promise.registered();
                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }

    private static void doBind0(final ChannelFuture regFuture, final Channel channel,
                                final SocketAddress localAddress, final ChannelPromise promise) {
        //绑定也是异步操作
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                //在这里仍要判断一次服务端的channel是否注册成功
                if (regFuture.isSuccess()) {
                    //注册成功之后开始绑定
                    channel.bind(localAddress, promise);
                } else {
                    //走到这里说明没有注册成功，把异常赋值给promise
                    promise.setFailure(regFuture.cause());
                }
            }
        });
    }

    final ChannelFuture initAndRegister() {
        Channel channel = null;
        //在这里初始化服务端channel，反射创建对象调用的无参构造器，
        //可以去NioServerSocketChannel类中看看无参构造器中做了什么
        channel = channelFactory.newChannel();
        //这里是异步注册的，一般来说，bossGroup设置的都是一个线程。
        ChannelFuture regFuture = bossGroup.next().register(channel);
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
         * @Author: PP-jessica
         * @Description:该方法简化一下， 全局的执行器不是必须引入的
         */
        @Override
        protected EventExecutor executor() {
            return super.executor();
        }
    }

}
