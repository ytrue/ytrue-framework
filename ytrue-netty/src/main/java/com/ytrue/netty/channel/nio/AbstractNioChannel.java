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

    @Override
    protected void doRegister() throws Exception {
        // 在这里把channel注册到单线程执行器中的selector上,注意这里的第三个参数this，这意味着channel注册的时候把本身，也就是nio类的channel
        // 当作附件放到key上了，之后会用到这个。
        selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
    }

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

    @Override
    public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        try {
            boolean doConnect = doConnect(remoteAddress, localAddress);
            if (!doConnect) {
                //这里的代码会搞出一个bug，我会在第六个版本的代码中修正，同时也会给大家讲一下bug是怎么产生的。这个bug只会在收发数据时
                //体现出来，所以并不会影响我们本节课的测试。我们现在还没有开始收发数据
                promise.trySuccess();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception;


    /**
     * 该方法本来在该类的静态内部类AbstractNioUnsafe中，这里先定义在这里,定义成一个抽象方法
     */
    protected abstract void read();
}
