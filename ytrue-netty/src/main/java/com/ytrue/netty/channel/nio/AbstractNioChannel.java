package com.ytrue.netty.channel.nio;

import com.ytrue.netty.channel.*;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.ConnectionPendingException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-07-26 10:11
 * @description AbstractNioChannel
 * <p>
 * - AbstractNioChannel是基于NIO的Channel的抽象实现类，它继承自AbstractChannel。
 * - AbstractNioChannel实现了基于NIO的Channel的一些公共功能，如底层的事件循环、ChannelPipeline的管理、ChannelConfig的设置等。
 * - 它提供了一些模板方法，供具体的NIO Channel实现类进行重写，以处理具体的NIO事件，如读取数据、写入数据、连接建立等。
 * - AbstractNioChannel还提供了一些辅助方法，用于管理底层的Selector和SelectionKey等。
 */
public abstract class AbstractNioChannel extends AbstractChannel {

    /**
     * 该抽象类是serversocketchannel和socketchannel的公共父类
     */
    private final SelectableChannel ch;


    /**
     * channel关注的读事件
     */
    protected final int readInterestOp;


    /**
     * channel注册到selector后返回的key
     */
    volatile SelectionKey selectionKey;

    /**
     * 是否还有未读取的数据
     */
    boolean readPending;


    private ChannelPromise connectPromise;
    private ScheduledFuture<?> connectTimeoutFuture;
    private SocketAddress requestedRemoteAddress;

    public AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent);
        this.ch = ch;
        this.readInterestOp = readInterestOp;
        try {
            //设置服务端channel为非阻塞模式
            ch.configureBlocking(false);
        } catch (IOException e) {
            try {
                //有异常直接关闭channel
                ch.close();
            } catch (IOException e2) {
                throw new RuntimeException(e2);
            }
            throw new RuntimeException("Failed to enter non-blocking mode.", e);
        }
    }

    @Override
    public boolean isOpen() {
        return ch.isOpen();
    }

    /**
     * 获取jdk的channel
     *
     * @return
     */
    protected SelectableChannel javaChannel() {
        return ch;
    }

    @Override
    public NioEventLoop eventLoop() {
        return (NioEventLoop) super.eventLoop();
    }

    /**
     * 获取SelectionKey
     *
     * @return
     */
    protected SelectionKey selectionKey() {
        assert selectionKey != null;
        return selectionKey;
    }

    /**
     * 判断 loop 是不是 NioEventLoop
     *
     * @param loop
     * @return
     */
    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof NioEventLoop;
    }

    protected final void clearReadPending() {
        if (isRegistered()) {
            EventLoop eventLoop = eventLoop();
            if (eventLoop.inEventLoop(Thread.currentThread())) {
                clearReadPending0();
            } else {
                eventLoop.execute(clearReadPendingRunnable);
            }
        } else {
            readPending = false;
        }
    }

    private final Runnable clearReadPendingRunnable = this::clearReadPending0;


    private void clearReadPending0() {
        readPending = false;
        ((AbstractNioUnsafe) unsafe()).removeReadOp();
    }

    /**
     * 获取NioUnsafe
     *
     * @return
     */
    @Override
    public NioUnsafe unsafe() {
        return (NioUnsafe) super.unsafe();
    }


    public interface NioUnsafe extends Unsafe {
        /**
         * 获取serversocketchannel和socketchannel的公共父类
         *
         * @return
         */
        SelectableChannel ch();

        /**
         * 完成连接
         */
        void finishConnect();

        /**
         * 读取
         */
        void read();

        /**
         * 用于强制刷新底层的发送缓冲区，将缓冲区中的数据立即发送给远程对等端。
         * 它的作用是确保数据被立即发送，而不是等待缓冲区满或其他条件满足时再发送。
         */
        void forceFlush();
    }

    /**
     * 终于又引入了一个unsafe的抽象内部类
     */
    protected abstract class AbstractNioUnsafe extends AbstractUnsafe implements NioUnsafe {


        /**
         * @Author: ytrue
         * @Description:从感兴趣的集合中删除读事件
         */
        protected final void removeReadOp() {
            SelectionKey key = selectionKey();
            if (!key.isValid()) {
                return;
            }
            int interestOps = key.interestOps();
            if ((interestOps & readInterestOp) != 0) {
                key.interestOps(interestOps & ~readInterestOp);
            }
        }

        @Override
        public SelectableChannel ch() {
            return javaChannel();
        }

        @Override
        public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            //查看通道是否打开
            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            try {
                //该值不为空，说明已经有连接存在了，不能再次连接
                if (connectPromise != null) {
                    throw new ConnectionPendingException();
                }
                //现在还不是活跃状态
                boolean wasActive = isActive();
                //这里会返回false
                if (doConnect(remoteAddress, localAddress)) {
                    fulfillConnectPromise(promise, wasActive);
                } else {
                    //可以为本次连接设置定时任务，检查是否连接超时了
                    connectPromise = promise;
                    requestedRemoteAddress = remoteAddress;

                    //这个getConnectTimeoutMillis是用户在客户端启动时配置的参数，如果没有配置，肯定会有一个默认的
                    //默认的超时时间是30s
                    int connectTimeoutMillis = config().getConnectTimeoutMillis();
                    if (connectTimeoutMillis > 0) {
                        //创建一个超时任务，如果在限定时间内没被取消，就去执行该任务，说明连接超时了，然后关闭channel
                        //在finishConnect()和doClose()中，该任务会被取消。就是连接完成或者通道关闭了，不需要再去检测了。
                        connectTimeoutFuture = eventLoop().schedule(() -> {
                            ChannelPromise connectPromise = AbstractNioChannel.this.connectPromise;
                            // 创建连接异常
                            ConnectException cause = new ConnectException("connection timed out: " + remoteAddress);
                            // 这里去尝试设置失败
                            if (connectPromise != null && connectPromise.tryFailure(cause)) {
                                //走到这里意味着连接超时，通道就会关闭
                                close(voidPromise());
                            }
                        }, connectTimeoutMillis, TimeUnit.MILLISECONDS);

                    }

                    // 监听器处理
                    promise.addListener((ChannelFutureListener) future -> {
                        //监听器，判断该future是否被取消了，如果被取消了，那就取消该定时任务，然后关闭channel
                        if (future.isCancelled()) {
                            if (connectTimeoutFuture != null) {
                                connectTimeoutFuture.cancel(false);
                            }
                            connectPromise = null;
                            close(voidPromise());
                        }
                    });

                }

            } catch (Throwable t) {
                promise.tryFailure(t);
                closeIfClosed();
            }
        }


        /**
         * 不是打开的就关闭掉
         */
        protected final void closeIfClosed() {
            if (isOpen()) {
                return;
            }
            close(voidPromise());
        }

        /**
         * 完成连接
         *
         * @param promise
         * @param wasActive
         */
        private void fulfillConnectPromise(ChannelPromise promise, boolean wasActive) {
            // 校验
            if (promise == null) {
                return;
            }
            // 获取活跃状态，这时候是成功的
            boolean active = isActive();
            // 设置成功
            boolean promiseSet = promise.trySuccess();

            if (!wasActive && active) {
                // 调用流水线激活事件
                pipeline().fireChannelActive();
            }

            if (!promiseSet) {
                close(voidPromise());
            }
        }

        /**
         * 在要实现这个方法了，仍然是简单实现，以后会完善至源码的程度
         */
        @Override
        public final void finishConnect() {
            assert eventLoop().inEventLoop(Thread.currentThread());
            try {
                //这里返回是false
                boolean wasActive = isActive();
                doFinishConnect();
                fulfillConnectPromise(connectPromise, wasActive);
            } catch (Throwable t) {
                //fulfillConnectPromise(connectPromise, annotateConnectException(t, requestedRemoteAddress));
            } finally {
                //检查是否为null，如果不等于null，则说明创建定时任务了，这时候已经连接完成，只要取消该任务就行
                if (connectTimeoutFuture != null) {
                    connectTimeoutFuture.cancel(false);
                }
                connectPromise = null;
            }
        }

        /**
         * @Author:ytrue
         * @Description:这个方法会在封装的flushTask中被调用，也就意味着这个方法是在socket可写的情况下被调用的 和注册的write事件是冲突的。所以下面才会判断，如果注册了write事件，就不会调用 super.flush0()
         * 没有注册write事件，才会执行这个异步任务，刷新数据到socket缓冲区中
         */
        @Override
        protected final void flush0() {
            //判断有没有注册write事件，没有注册才会继续向下执行
            if (!isFlushPending()) {
                super.flush0();
            }
        }


        /**
         * @Author:ytrue
         * @Description:该方法用来判断是否有数据待刷新，就是是否注册了write事件
         */
        private boolean isFlushPending() {
            SelectionKey selectionKey = selectionKey();
            return selectionKey.isValid() && (selectionKey.interestOps() & SelectionKey.OP_WRITE) != 0;
        }


        /**
         * 强制刷新消息的方法，当selector接收到write事件时，就会调用这个方法，强制把写缓冲区的消息刷新到socket中
         * 这里其实还是会调用父类的flush0方法
         */
        @Override
        public final void forceFlush() {
            super.flush0();
        }


    }

    /**
     * channel注册
     *
     * @throws Exception
     */
    @Override
    protected void doRegister() throws Exception {
        //在这里把channel注册到单线程执行器中的selector上,注意这里的第三个参数this，这意味着channel注册的时候把本身，也就是nio类的channel
        //当作附件放到key上了，之后会用到这个。
        selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
    }

    /**
     * 设置感兴趣的事件
     *
     * @throws Exception
     */
    @Override
    protected void doBeginRead() throws Exception {
        final SelectionKey selectionKey = this.selectionKey;
        //检查key是否是有效的
        if (!selectionKey.isValid()) {
            return;
        }
        //还没有设置感兴趣的事件，所以得到的值为0
        final int interestOps = selectionKey.interestOps();
        //interestOps中并不包含readInterestOp
        if ((interestOps & readInterestOp) == 0) {
            //设置channel关注的事件，读事件
            selectionKey.interestOps(interestOps | readInterestOp);
        }
    }


    /**
     * 完成连接操作
     *
     * @throws Exception
     */
    protected abstract void doFinishConnect() throws Exception;

    /**
     * 真正连接操作
     *
     * @param remoteAddress
     * @param localAddress
     * @return
     * @throws Exception
     */
    protected abstract boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception;


    /**
     * 该方法先不实现，在引入了channelHandler后会实现
     *
     * @throws Exception
     */
    @Override
    protected void doClose() throws Exception {
    }

}
