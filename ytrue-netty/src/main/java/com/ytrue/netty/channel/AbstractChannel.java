package com.ytrue.netty.channel;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;

/**
 * @author ytrue
 * @date 2023-07-26 9:32
 * @description channel接口的抽象实现类，这里面有许多重要方法没有实现，有很多方法没有引进，后续依次引入
 * 该类中的bind，close等等方法，都是定义好的模版方法，在子类中有真正的被调用的实现方法，以doxxxx开头。
 * <p>
 * - AbstractChannel是通用的Channel的抽象实现类，它实现了Channel接口，并提供了一些通用的功能和行为。
 * - AbstractChannel实现了Channel的基本方法，如bind()、connect()、write()、flush()等。
 * - 它管理了ChannelPipeline，用于处理输入和输出的数据流，以及执行ChannelHandler的逻辑。
 * - AbstractChannel还提供了一些辅助方法，用于管理Channel的状态、属性、事件等。
 */
public abstract class AbstractChannel implements Channel {

    /**
     * 当创建的是客户端channel时，parent为serversocketchannel,如果创建的为服务端channel，parent则为null
     */
    private final Channel parent;

    /**
     * 在客户端连接建立后，生成Channel通道的时候会为每一个Channel分配一个唯一的ID
     */
    private final ChannelId id;


    /**
     * 看名字也可以猜出，这个future是在channel关闭的时候使用的，是一个静态内部类
     */
    private final CloseFuture closeFuture = new CloseFuture(this);

    /**
     * 获取当前Channel的本地绑定地址
     */
    private volatile SocketAddress localAddress;

    /**
     * 获取当前Channel通信的远程Socket地址
     */
    private volatile SocketAddress remoteAddress;

    /**
     * 初始化关闭异常
     */
    private Throwable initialCloseCause;

    /**
     * 每一个channel都要绑定到一个eventloop上
     */
    private volatile EventLoop eventLoop;

    /**
     * 该channel是否注册过
     */
    private volatile boolean registered;

    protected AbstractChannel(Channel parent) {
        this.parent = parent;
        id = newId();
    }

    @Override
    public ChannelId id() {
        return id;
    }

    @Override
    public EventLoop eventLoop() {
        EventLoop eventLoop = this.eventLoop;
        if (eventLoop == null) {
            throw new IllegalStateException("channel not registered to an event loop");
        }
        return eventLoop;
    }

    @Override
    public Channel parent() {
        return parent;
    }

    @Override
    public ChannelConfig config() {
        return null;
    }


    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public SocketAddress localAddress() {
        return null;
    }

    @Override
    public SocketAddress remoteAddress() {
        return null;
    }

    @Override
    public ChannelFuture closeFuture() {
        return closeFuture;
    }

    @Override
    public ChannelFuture close() {
        return null;
    }

    /**
     * 判断 loop 是不是 NioEventLoop
     *
     * @param loop
     * @return
     */
    protected abstract boolean isCompatible(EventLoop loop);


    /**
     * 下面三个方法都不在此处，而是在该类的静态内部类AbstractUnsafe中，现在先放在这里
     *
     * @throws Exception
     */
    protected void doRegister() throws Exception {
    }

    protected abstract void doBeginRead() throws Exception;

    protected abstract void doBind(SocketAddress localAddress) throws Exception;


    /**
     * 该方法并不在此类，而是在该类的静态内部类AbstractUnsafe中，现在先放在这里
     * 在这里多说一句，越到后面抽象类越多，看源码的时候常常会发现抽象父类调用子类的方法看着看着就晕了，我最开始学看源码的时候就这样
     * 后来看得多了，我给自己总结了一句话：看抽象类的时候只要记住我们最终创建的那个类是各个抽象类和各种接口的最终子类，一直记着这句话
     * 看源码时候就会清楚很多，不管抽象父类怎么调用子类方法，实际上都是在我们创建的最终子类中调来调去。
     *
     * @param eventLoop
     * @param promise
     */
    @Override
    public void register(EventLoop eventLoop, ChannelPromise promise) {
        // 校验
        if (eventLoop == null) {
            throw new NullPointerException("eventLoop");
        }

        // 检查channel是否注册过，注册过就手动设置promise失败
        if (isRegistered()) {
            promise.setFailure(new IllegalStateException("registered to an event loop already"));
            return;
        }

        // 判断当前使用的执行器是否为NioEventLoop，如果不是手动设置失败
        if (!isCompatible(eventLoop)) {
            promise.setFailure(new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
            return;
        }

        // 稍微学过netty的人都知道，一个channel绑定一个单线程执行器。终于在这里，我们看到channel绑定了单线程执行器
        // 接着channel，不管是客户端还是服务端的，会把自己注册到绑定的单线程执行器中的selector上
        AbstractChannel.this.eventLoop = eventLoop;
        //又看到这个方法了，又一次说明在netty中，channel注册，绑定，连接等等都是异步的，由单线程执行器来执行
        if (eventLoop.inEventLoop(Thread.currentThread())) {
            register0(promise);
        } else {
            try {
                //如果调用该放的线程不是netty的线程，就封装成任务由线程执行器来执行
                eventLoop.execute(() -> register0(promise));
            } catch (Throwable t) {
                System.out.println(t.getMessage());
                //该方法先不做实现，等引入unsafe之后会实现
                //closeForcibly();
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }
    }

    /**
     * 该方法并不在此类，而是在该类的静态内部类AbstractUnsafe中，现在先放在这里
     *
     * @param promise
     */
    private void register0(ChannelPromise promise) {
        try {
            // 如果标记当前的Promise实例为不可取消，设置成功返回true，否则返回false
            // 如果设置失败，或者不是打开的，就不处理了
            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }
            //真正的注册方法
            doRegister();
            //修改注册状态
            registered = true;
            //把成功状态赋值给promise，这样它可以通知回调函数执行
            //我们在之前注册时候，把bind也放在了回调函数中
            safeSetSuccess(promise);
            //在这里给channel注册读事件
            beginRead();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public final void beginRead() {
        //如果是服务端的channel，这里仍然可能为false
        //那么真正注册读事件的时机，就成了绑定端口号成功之后
        if (!isActive()) {
            return;
        }
        try {
            doBeginRead();
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {
        try {
            doBind(localAddress);
            safeSetSuccess(promise);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 确保channel是打开的
     *
     * @param promise
     * @return
     */
    protected final boolean ensureOpen(ChannelPromise promise) {
        // 判断是不是打开的
        if (isOpen()) {
            return true;
        }
        // 设置promise为失败，抛出关闭异常
        safeSetFailure(promise, newClosedChannelException(initialCloseCause));
        return false;
    }

    /**
     * 创建ClosedChannelException异常
     *
     * @param cause
     * @return
     */
    private ClosedChannelException newClosedChannelException(Throwable cause) {
        ClosedChannelException exception = new ClosedChannelException();
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    /**
     * 设置成功
     *
     * @param promise
     */
    protected final void safeSetSuccess(ChannelPromise promise) {
        if (!promise.trySuccess()) {
            System.out.println("Failed to mark a promise as success because it is done already: " + promise);
        }
    }

    /**
     * 该方法其实也在unsafe类中
     *
     * @param promise
     * @param cause
     */
    protected final void safeSetFailure(ChannelPromise promise, Throwable cause) {
        if (!promise.tryFailure(cause)) {
            throw new RuntimeException(cause);
        }
    }


    /**
     * 创建channelId
     *
     * @return
     */
    private ChannelId newId() {
        return DefaultChannelId.newInstance();
    }


    static final class CloseFuture extends DefaultChannelPromise {

        public CloseFuture(Channel channel) {
            super(channel);
        }

        @Override
        public ChannelPromise setSuccess() {
            throw new IllegalStateException();
        }

        @Override
        public ChannelPromise setFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        @Override
        public boolean trySuccess() {
            throw new IllegalStateException();
        }

        @Override
        public boolean tryFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        /**
         * 设置关闭
         *
         * @return
         */
        boolean setClosed() {
            return super.trySuccess();
        }
    }
}
