package com.ytrue.netty.channel.nio;

import com.ytrue.netty.channel.AbstractChannel;
import com.ytrue.netty.channel.Channel;
import com.ytrue.netty.channel.ChannelPromise;
import com.ytrue.netty.channel.EventLoop;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

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

        @Override
        public SelectableChannel ch() {
            return javaChannel();
        }

        @Override
        public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            try {
                boolean doConnect = doConnect(remoteAddress, localAddress);
                if (!doConnect) {
                    //在这里直接就唤醒的话，也许客户端的channel还没连接成功，在源码中并不是这样处理的，源码中有一个定时任务
                    //定时任务的时间到了之后会去检查连接是否成功了没，成功了才会让客户端程序继续向下运行，后面我们会进一步完善代码
                    //把这里注释了，然后阻塞住主线程，反而可以发送成功，这说明就是这里出的问题，所以，好好想想，问题出在哪里。
                    //没错，认真想想netty的线程模型，如果在这里threadsleep，阻塞的究竟是谁呢？是单线程执行器，因为这个方法本身就在被单线程
                    //执行器执行了，所以在这里阻塞是没用的。阻塞在这里，就意味着单线程执行器不能继续工作，不能select.selector，无法处理关注的事件
                    //自然也就无法真正的连接成功。那么只要这里阻塞的时间一过，执行器就会继续执行，然后设置成功状态，主线程继续向下执行，可这时候执行器也许
                    //刚开始下一轮select.selector，然后处理连接事件，所以就会又一次发送消息失败
                    //粗暴地在客户端代码中阻塞主线程，这样即留给了单线程执行器循环下一轮时间，主线程阻塞3秒后会自动醒来，那时候
                    //一切都准备就绪，可以发送消息了
                    //那么源码中是怎么做的呢？作者使用了一个定时任务，经过一定的时间后，定时任务被单线程执行器执行，在定时任务中，作者采取了一些步骤去检验
                    //客户端有没有连接成功，成功的话就把一并传进定时任务的promise设置为成功状态，然后调用了sync()方法的主线程就被唤醒，而且一切准备就绪，
                    //可以直接发送数据喽。
                    //Thread.sleep(3000);
                    promise.trySuccess();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        /**
         * 暂时不做实现,现在要实现这个方法了，仍然是简单实现，以后会完善至源码的程度
         */
        @Override
        public final void finishConnect() {
            assert eventLoop().inEventLoop(Thread.currentThread());
            try {
                //真正处理连接完成的方法
                doFinishConnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 暂时不做实现
         */
        @Override
        public final void forceFlush() {
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
