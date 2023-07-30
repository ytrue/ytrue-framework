package com.ytrue.netty.channel;

import com.ytrue.netty.util.DefaultAttributeMap;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.RejectedExecutionException;

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
@Slf4j
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {

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

    /**
     * 加入unsafe属性
     */
    private final Unsafe unsafe;

    /**
     * pipeline
     */
    private final DefaultChannelPipeline pipeline;

    protected AbstractChannel(Channel parent) {
        this.parent = parent;
        unsafe = newUnsafe();
        id = newId();
        pipeline = newChannelPipeline();
    }


    protected AbstractChannel(Channel parent, ChannelId id) {
        this.parent = parent;
        this.id = id;
        unsafe = newUnsafe();
        pipeline = newChannelPipeline();
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
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

    // 在源码中config类并不在这里实现，而是放在了niosocketchannel和nioserversocketchannel中分别实现，
    // 这么做是因为客户端和服务端的配置并相同，所以要分别作处理，这也是定义公共接口子类各自实现的一种体现
//    @Override
//    public ChannelConfig config() {
//        return null;
//    }

    /**
     * 创建Pipeline
     * @return
     */
    protected DefaultChannelPipeline newChannelPipeline() {
        //把创建出的channel传入DefaultChannelPipeline；
        return new DefaultChannelPipeline(this);
    }


    /**
     * 是否已注册
     *
     * @return
     */
    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public SocketAddress localAddress() {
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            try {
                this.localAddress = localAddress = unsafe().localAddress();
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                return null;
            }
        }

        return localAddress;
    }

    @Override
    public SocketAddress remoteAddress() {
        SocketAddress remoteAddress = this.remoteAddress;
        if (remoteAddress == null) {
            try {
                this.remoteAddress = remoteAddress = unsafe().remoteAddress();
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                return null;
            }
        }
        return remoteAddress;
    }

    @Override
    public ChannelFuture closeFuture() {
        return closeFuture;
    }

    @Override
    public Channel read() {
        // DefaultChannelPipeline#read--》 最终调用HeadContext#read，而它调用的是 unsafe.beginRead
        pipeline.read();
        return this;
    }

    @Override
    public Channel flush() {
        return null;
    }

    // 下面是 ChannelOutboundInvoker 接口方法
    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture disconnect() {
        return null;
    }

    @Override
    public ChannelFuture close() {
        return null;
    }

    @Override
    public ChannelFuture deregister() {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        // DefaultChannelPipeline#bind--》 最终调用HeadContext#bind，而它调用的是 unsafe.bind(localAddress, promise);
        return pipeline.bind(localAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        // DefaultChannelPipeline#connect--》 最终调用HeadContext#connect，而它调用的是 connect.bind(remoteAddress, promise);
        return pipeline.connect(remoteAddress, promise);
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture write(Object msg) {
        return null;
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return pipeline.writeAndFlush(msg);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelPromise newPromise() {
        return null;
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return null;
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return null;
    }
    // ChannelOutboundInvoker 接口方法结束

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    /**
     * 判断 loop 是不是 NioEventLoop
     *
     * @param loop
     * @return
     */
    protected abstract boolean isCompatible(EventLoop loop);

    /**
     * 创建channelId
     *
     * @return
     */
    private ChannelId newId() {
        return DefaultChannelId.newInstance();
    }

    /**
     * 创建Unsafe
     *
     * @return
     */
    protected abstract AbstractUnsafe newUnsafe();

    /**
     * Unsafe的抽象内部类，看到上面一下子多了那么多方法，而且还有很多是没实现的，一定会感到很懵逼
     * 但是，请记住这句话，在抽象类中，很多方法尽管实现了方法体，内在逻辑却并不复杂。因为这些方法几乎都是为子类服务的，
     * 换句话说，就是定义了一些模版方法而已，真正实现的方法在子类之中。就比调用了AbstractChannel抽象类的bind方法，
     * 在该方法内部，又会调用unsafe抽象内部类的bind方法，而该内部类的bind方法又会调用AbstractChannel类中的另一个抽象方法
     * doBind，虽然NioServerChannel中也有该方法的实现，但该方法在子类NioServerSocketChannel中才是真正实现。
     * 我想，这时候有的朋友可能又会困惑作者为什么这样设计类的继承结构。也许有的朋友已经清楚了，但我在这里再啰嗦一句，
     * 首先，channel分为客户端和服务端，因为抽象出了公共的接口和父抽象类，两种channel不得不实现相同的方法，
     * 那么不同的channel实现的相同方法的逻辑应该不同，所以dobind设计为抽象方法是很合理的。因为你不能让NiosocketChannel客户端channel
     * 向服务端channel那样去绑定端口，虽然要做也确实可以这么做。。
     * 按照我的这种思路，大家可以思考思考，为什么继承了同样的接口，有的方法可以出现在这个抽象类中，有的方法可以出现在那个抽象类中，为什么有的方法要设计成抽象的
     */
    protected abstract class AbstractUnsafe implements Unsafe {

        private boolean neverRegistered = true;

        /**
         * 断言是不是EventLoop
         */
        private void assertEventLoop() {
            assert !registered || eventLoop.inEventLoop(Thread.currentThread());
        }

        /**
         * 获取当前Channel的本地绑定地址
         *
         * @return
         */
        @Override
        public final SocketAddress localAddress() {
            return localAddress0();
        }

        /**
         * 获取当前Channel通信的远程Socket地址
         *
         * @return
         */
        @Override
        public SocketAddress remoteAddress() {
            return remoteAddress0();
        }

        /**
         * 注册channel
         *
         * @param eventLoop
         * @param promise
         */
        @Override
        public void register(EventLoop eventLoop, ChannelPromise promise) {
            if (eventLoop == null) {
                throw new NullPointerException("envLoop is null");
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

            //稍微学过netty的人都知道，一个channel绑定一个单线程执行器。终于在这里，我们看到channel绑定了单线程执行器
            //接着channel，不管是客户端还是服务端的，会把自己注册到绑定的单线程执行器中的selector上
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
         * 注册
         *
         * @param promise
         */
        private void register0(ChannelPromise promise) {
            try {
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }
                boolean firstRegistration = neverRegistered;
                //真正的注册方法
                doRegister();
                //修改注册状态
                neverRegistered = false;
                registered = true;
                //回调链表中的方法，链表中的每一个节点都会执行它的run方法，在run方法中
                //ChannelPipeline中每一个节点中handler的handlerAdded方法，在执行callHandlerAdded的时候，handler的添加状态
                //更新为ADD_COMPLETE
                pipeline.invokeHandlerAddedIfNeeded();
                //把成功状态赋值给promise，这样它可以通知回调函数执行
                //我们在之前注册时候，把bind也放在了回调函数中
                safeSetSuccess(promise);
                //channel注册成功后回调每一个handler的channelRegister方法
                pipeline.fireChannelRegistered();
                //这里通道还不是激活状态，因为还未绑定端口号，所以下面的分支并不会进入
                //等绑定了端口号之后，还会执行一次 pipeline.fireChannelActive方法，
                //这时候每一个handler中的ChannelActive方法将会被回调
                //如果是服务端接受的客户端channel注册了工作线程组的多路复用器之后，这时候被接受的客户端channel已经是
                //处于激活状态了，这里是可以回调成功的
                if (isActive()) {
                    if (firstRegistration) {
                        //触发channelActive回调
                        pipeline.fireChannelActive();
                    } else if (config().isAutoRead()) {
                        //在这里有可能无法关注读事件
                        beginRead();
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        /**
         * 将Channel绑定到指定的本地地址
         *
         * @param localAddress
         * @param promise
         */
        @Override
        public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {
            //这里一定为false，因为channel还未绑定端口号，肯定不是激活状态
            boolean wasActive = isActive();
            try {
                doBind(localAddress);
            } catch (Exception e) {
                safeSetFailure(promise, e);
            }
            //这时候一定为true了
            if (!wasActive && isActive()) {
                //然后会向单线程执行器中提交任务，任务重会执行ChannelPipeline中每一个节点中handler的ChannelActive方法
                invokeLater(() -> pipeline.fireChannelActive());
            }

            safeSetSuccess(promise);
        }

        @Override
        public final void disconnect(final ChannelPromise promise) {
        }

        @Override
        public final void close(final ChannelPromise promise) {
        }

        @Override
        public final void closeForcibly() {
            assertEventLoop();
            try {
                doClose();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        @Override
        public final void deregister(final ChannelPromise promise) {
        }

        @Override
        public final void beginRead() {
            assertEventLoop();
            //如果是服务端的channel，这里仍然可能为false
            //那么真正注册读事件的时机，就成了绑定端口号成功之后
            if (!isActive()) {
                return;
            }
            try {
                doBeginRead();
            } catch (final Exception e) {
                //如果出现了异常，就提交异步任务，在任务中抓住异常
                invokeLater(() -> pipeline.fireExceptionCaught(e));
                close(closeFuture);
            }
        }

        /**
         * 这个方法在上一节课没有实现，在这我们也不会真正的实现它，但是可以在这里稍微加点东西，讲解一下发送消息的逻辑和步骤
         * ByteBuf讲完了之后，我们再重写该方法。在这里，该方法只是把数据发送到了一个netty自定义的缓冲区中，还没有放入socket的缓冲区
         * 真正的write方法在子类NioSocketChannel中实现，在那个方法中，数据才被真正放进socket的缓冲区中
         * 根据之前的老套路，仍需要在AbstractChannel抽象类中定义一个抽象方法，让子类去实现
         *
         * @param msg
         * @param promise
         */
        @Override
        public final void write(Object msg, ChannelPromise promise) {
            try {
                doWrite(msg);
                //如果有监听器，这里可以通知监听器执行回调方法
                promise.trySuccess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public final void flush() {
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
         * 设置成功
         *
         * @param promise
         */
        protected final void safeSetSuccess(ChannelPromise promise) {
            if (!promise.trySuccess()) {
                log.warn("Failed to mark a promise as success because it is done already: {}", promise);
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
                log.warn("Failed to mark a promise as failure because it's done already: {}", promise, cause);
            }
        }

        private void invokeLater(Runnable task) {
            try {
                eventLoop().execute(task);
            } catch (RejectedExecutionException e) {
                log.warn("Can't invoke task later as EventLoop rejected it", e);
            }
        }
    }

    /**
     * 该方法的位置正确，但是参数和源码有差异，源码的参数是netty自己定义的ChannelOutboundBuffer，是出站的数据缓冲区现在，我们先用最简单的实现，之后再重写
     *
     * @param msg
     * @throws Exception
     */
    protected abstract void doWrite(Object msg) throws Exception;

    /**
     * 获取当前Channel的本地绑定地址，交给子类实现
     *
     * @return
     */
    protected abstract SocketAddress localAddress0();

    /**
     * 获取当前Channel通信的远程Socket地址 交给子类实现
     *
     * @return
     */
    protected abstract SocketAddress remoteAddress0();


    /**
     * 真正注册channel
     *
     * @throws Exception
     */
    protected void doRegister() throws Exception {
    }

    /**
     * 真正绑定
     *
     * @param localAddress
     * @throws Exception
     */
    protected abstract void doBind(SocketAddress localAddress) throws Exception;

    /**
     * 真正的关闭
     *
     * @throws Exception
     */
    protected abstract void doClose() throws Exception;


    /**
     * 设置感兴趣的事件
     *
     * @throws Exception
     */
    protected abstract void doBeginRead() throws Exception;

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
     * CloseFuture
     */
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
