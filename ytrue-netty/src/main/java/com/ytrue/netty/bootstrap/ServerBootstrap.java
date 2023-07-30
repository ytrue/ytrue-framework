package com.ytrue.netty.bootstrap;

import com.ytrue.netty.channel.*;
import com.ytrue.netty.test.TestHandlerOne;
import com.ytrue.netty.util.AttributeKey;
import com.ytrue.netty.util.internal.ObjectUtil;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-07-28 15:30
 * @description ServerBootstrap
 */
@Slf4j
@NoArgsConstructor
public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, Channel> {

    private EventLoopGroup bossGroup;


    /**
     * 用户设定的NioSocketChannel的参数会暂时存放在这个map中，channel初始化的时候，这里面的数据才会存放到channel的配置类中
     */
    private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap<>();

    /**
     * 用户设定的NioSocketChannel的参数会暂时存放在这个map中，channel初始化的时候，这里面的数据才会存放到channel的配置类中
     */
    private final Map<AttributeKey<?>, Object> childAttrs = new LinkedHashMap<>();

    private final ServerBootstrapConfig config = new ServerBootstrapConfig(this);

    private EventLoopGroup childGroup;

    private volatile ChannelFactory<? extends Channel> channelFactory;


    /**
     * 在服务端设定的客户端的handler
     */
    private volatile ChannelHandler childHandler;

    @Override
    void init(Channel channel) throws Exception {
        //得到所有存储在map中的用户设定的channel的参数
        final Map<ChannelOption<?>, Object> options = options0();
        synchronized (options) {
            // 把初始化时用户配置的参数全都放到channel的config类中，因为没有引入netty源码的打印日志模块，
            // 所以就把该方法修改了，去掉了日志参数
            setChannelOptions(channel, options);
        }
        final Map<AttributeKey<?>, Object> attrs = attrs0();
        synchronized (attrs) {
            for (Map.Entry<AttributeKey<?>, Object> e : attrs.entrySet()) {
                @SuppressWarnings("unchecked") AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
                channel.attr(key).set(e.getValue());
            }
        }

        ChannelPipeline p = channel.pipeline();
        final EventLoopGroup currentChildGroup = childGroup;
        final ChannelHandler currentChildHandler = childHandler;
        final Map.Entry<ChannelOption<?>, Object>[] currentChildOptions;
        final Map.Entry<AttributeKey<?>, Object>[] currentChildAttrs;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(newOptionArray(0));
        }
        synchronized (childAttrs) {
            currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(0));
        }
        //这里要给NioServerSocketChannel的ChannelPipeline添加一个handler节点，一定要理清这里的逻辑，这个逻辑十分重要
        //首先，我们可以看到这个handler是ChannelInitializer类型的，而且添加进去之后，该节点还不是已添加状态，只有当channel注册单线程
        //执行器成功后，该handler的handlerAdded才会被回调，回调的过程中会讲handler的状态改为添加完成。
        //然后我们再看看该handler的handlerAdded方法逻辑，我们会发现handlerAdded方法会执行到initChannel方法中，就是下面的重写方法
        //在重写方法内，会再次拿出NioServerSocketChannel的ChannelPipeline，向ChannelPipeline中添加用户自己向ChannelPipeline中设置
        //的handler，如果设置了多个handler，那么该handler肯定是ChannelInitializer类型的，这时候执行了pipeline.addLast(handler)代码
        //而在该代码中，调用逻辑又会来到ChannelPipeline的addLast方法中，但是，这次channel已经注册成功，不必再封装回调链表，可以直接执行
        //callHandlerAdded0方法，这样一来，就直接会回调到ChannelInitializer中的handlerAdded方法，今儿又会执行到重写的initChannel
        //方法中，这时候，才会把用户设置的多个handler真正添加到ChannelPipeline中。到这里， pipeline.addLast(handler)这一行代码的逻辑才真正完成。

        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(final Channel ch) {
                final ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }
                //紧接着在这里，又封装了个异步任务，提交到单线程执行器中。逻辑已经很简单了，单线程执行器的线程会向ChannelPipeline
                //中添加handler，接下来该方法内的任何方法被回调了，都是由单线程执行器执行的。
                ch.eventLoop().execute(() -> pipeline.addLast(new ServerBootstrapAcceptor(
                        ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs)));
            }
        });

    }

    private ServerBootstrap(ServerBootstrap bootstrap) {
        super(bootstrap);
        childGroup = bootstrap.childGroup;
        childHandler = bootstrap.childHandler;
        synchronized (bootstrap.childOptions) {
            childOptions.putAll(bootstrap.childOptions);
        }
        synchronized (bootstrap.childAttrs) {
            childAttrs.putAll(bootstrap.childAttrs);
        }
    }

    @Override
    public ServerBootstrap group(EventLoopGroup group) {
        return group(group, group);
    }


    /**
     * 把boss线程组和工作线程组赋值给属性，并且把boss线程组传递到父类，这时候线程组都已经初始化完毕了
     * 里面的每个线程执行器也都初始化完毕
     *
     * @param parentGroup
     * @param childGroup
     * @return
     */
    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        super.group(parentGroup);
        ObjectUtil.checkNotNull(childGroup, "childGroup");
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        this.childGroup = childGroup;
        return this;
    }


    public ServerBootstrap childHandler(ChannelHandler childHandler) {
        this.childHandler = ObjectUtil.checkNotNull(childHandler, "childHandler");
        return this;
    }

    public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value) {
        ObjectUtil.checkNotNull(childOption, "childOption");
        if (value == null) {
            synchronized (childOptions) {
                childOptions.remove(childOption);
            }
        } else {
            synchronized (childOptions) {
                childOptions.put(childOption, value);
            }
        }
        return this;
    }

    public <T> ServerBootstrap childAttr(AttributeKey<T> childKey, T value) {
        ObjectUtil.checkNotNull(childKey, "childKey");
        if (value == null) {
            childAttrs.remove(childKey);
        } else {
            childAttrs.put(childKey, value);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private static Map.Entry<AttributeKey<?>, Object>[] newAttrArray(int size) {
        return new Map.Entry[size];
    }

    @SuppressWarnings("unchecked")
    private static Map.Entry<ChannelOption<?>, Object>[] newOptionArray(int size) {
        return new Map.Entry[size];
    }

    @Override
    public ServerBootstrap validate() {
        super.validate();
        //还没有引入channelHandler，先把这一段注释掉
//        if (childHandler == null) {
//            throw new IllegalStateException("childHandler not set");
//        }
        if (childGroup == null) {
            log.warn("childGroup is not set. Using parentGroup instead.");
            childGroup = config.group();
        }
        return this;
    }

    @Override
    public AbstractBootstrapConfig<ServerBootstrap, Channel> config() {
        return config;
    }

    public EventLoopGroup childGroup() {
        return childGroup;
    }

    private static class ServerBootstrapAcceptor extends ChannelInboundHandlerAdapter {

        /**
         * 下面这四个属性，分别都和客户端channel有关，也就是NioSocketChannel，
         * childHandler()
         * childOption().
         * childAttribute("")
         * childAttr()
         * 上面这几个方法应该非常熟悉吧
         */
        private final EventLoopGroup childGroup;
        private final ChannelHandler childHandler;
        private final Map.Entry<ChannelOption<?>, Object>[] childOptions;
        private final Map.Entry<AttributeKey<?>, Object>[] childAttrs;
        private final Runnable enableAutoReadTask;

        ServerBootstrapAcceptor(
                final Channel channel,
                EventLoopGroup childGroup,
                ChannelHandler childHandler,
                Map.Entry<ChannelOption<?>, Object>[] childOptions,
                Map.Entry<AttributeKey<?>, Object>[] childAttrs
        ) {
            this.childGroup = childGroup;
            this.childHandler = childHandler;
            this.childOptions = childOptions;
            this.childAttrs = childAttrs;
            enableAutoReadTask = () -> channel.config().setAutoRead(true);
        }


        /**
         * 当该方法被回调的时候，说明是有读事件进来了，而且该类只关注客户端channel连接和初始化。
         * 客户端channel连接进来也会调用read方法，可以去NioEventLoop类中产看调用逻辑
         * <p>
         * AbstractNioMessageChannel#read方法
         *
         * @param ctx
         * @param msg
         */
        @Override
        @SuppressWarnings("unchecked")
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            //接收到的其实就是已经实例化好的客户端channel，但是还没有完成初始化
            final Channel child = (Channel) msg;
            //childHandler是服务端为客户端channel设置的handler，这一步是向客户端的channel中添加handler
            child.pipeline().addLast(childHandler);
            //把用户设置的属性设置到客户端channel中
            setChannelOptions(child, childOptions);
            //NioSocketChannel也是个map，用户存储在map中的参数也要设置进去
            for (Map.Entry<AttributeKey<?>, Object> e : childAttrs) {
                child.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
            }
            //这一步最为重要，childGroup就是服务端设置的workgroup，这一步就是把接收到的客户端channel绑定到childGroup
            //中的一个单线程执行器上，绑定成功也会回调客户端channel中handler的相应方法，逻辑都是一样的。
            try {
                // socketChannel注册，注册是会绑定读事件的，具体看Unsafe#register
                childGroup.register(child).addListener((ChannelFutureListener) future -> {
                    //注册失败，则强制关闭channel
                    if (!future.isSuccess()) {
                        forceClose(child, future.cause());
                    }
                });
            } catch (Throwable t) {
                forceClose(child, t);
            }
        }

        private static void forceClose(Channel child, Throwable t) {
            child.unsafe().closeForcibly();
            log.warn("Failed to register an accepted channel: {}", child, t);
        }

        //该方法暂时注释掉，有我们没有引入的方法
//        @Override
//        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//            final ChannelConfig config = ctx.channel().config();
//            if (config.isAutoRead()) {
//                config.setAutoRead(false);
//                ctx.channel().eventLoop().schedule(enableAutoReadTask, 1, TimeUnit.SECONDS);
//            }
//            ctx.fireExceptionCaught(cause);
//        }

    }
}
