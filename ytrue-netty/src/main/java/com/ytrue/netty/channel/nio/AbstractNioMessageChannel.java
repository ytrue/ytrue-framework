package com.ytrue.netty.channel.nio;

import com.ytrue.netty.channel.*;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-07-26 10:29
 * @description AbstractNioMessageChannel
 */
public abstract class AbstractNioMessageChannel extends AbstractNioChannel {

    /**
     * 当该属性为true时，服务端将不再接受来自客户端的数据
     */
    boolean inputShutdown;


    public AbstractNioMessageChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent, ch, readInterestOp);
    }

    @Override
    protected AbstractNioUnsafe newUnsafe() {
        return new NioMessageUnsafe();
    }


    @Override
    protected void doBeginRead() throws Exception {
        if (inputShutdown) {
            return;
        }
        super.doBeginRead();
    }

    private final class NioMessageUnsafe extends AbstractNioUnsafe {

        /**
         * 存放服务端建立的客户端连接，该成员变量本来在NioMessageUnsafe静态内部类中
         */
        private final List<Object> readBuf = new ArrayList<>();

        @Override
        public void read() {
            //该方法要在netty的线程执行器中执行
            assert eventLoop().inEventLoop(Thread.currentThread());
            final ChannelConfig config = config();
            //得到ChannelPipeline
            final ChannelPipeline pipeline = pipeline();
            //得到动态内存分配的处理器，注意，这里得到的并不是一个分配器，而是一个handle
            final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();

            //把配置类传进去，这里方法的具体作用其实就是在再次开始接收客户端连接前，重置一下接收次数，就是把上一次接收连接的次数清零
            allocHandle.reset(config);

            boolean closed = false;
            Throwable exception = null;
            try {
                try {
                    do {
                        //开始接收客户端连接，这里只要接收到客户端连接了，就会返回1。如果返回的值为0，说明没有客户端连接可被接收
                        int localRead = doReadMessages(readBuf);
                        //这里就意味着已经没有客户端连接可被接收了，直接退出这个循环即可
                        if (localRead == 0) {
                            break;
                        }
                        if (localRead < 0) {
                            closed = true;
                            break;
                        }
                        //在这里累加接收到的客户端的连接次数
                        allocHandle.incMessagesRead(localRead);
                        //这里会判断接收连接的总次数是否达到16次了，如果达到了就要退出循环了
                        //其实不管是接收客户端连接还是读取消息，都是限制16次，这个限制的作用其实也很简单，就是把执行机会让出去
                        //不让这一个单线程执行器只干着一件事，还有其他的用户设定的任务也等待这个单线程执行器去执行呢。而且，这个单线程
                        //执行期管理着多个客户端连接，不能总读取这一个客户端连接的消息吧
                    } while (allocHandle.continueReading());
                } catch (Throwable t) {
                    exception = t;
                }
                //这里可以获得接收到的客户端连接的总个数
                int size = readBuf.size();
                for (int i = 0; i < size; i ++) {
                    //走到这里肯定就是没有可以继续接受的数据了，所以要把这个属性置为false
                    readPending = false;
                    //回调ChannelRead方法，在这个回调方法中，会把接收到的客户端连接注册到工作线程的selector上
                    //可以看到ChannelRead方法是每接收一个客户端连接就要被回调一次
                    pipeline.fireChannelRead(readBuf.get(i));
                }
                //清除接收到的客户端连接
                readBuf.clear();
                //这里这个方法其实没什么用，在读取客户端发送的消息时，这个方法会记录本次读取到的总的字节数
                //但是在接收客户端连接时，这个方法只是一个空实现
                allocHandle.readComplete();
                //回调ChannelReadComplete方法
                //这个方法只会被回调一次，在所有连接接收完了之后被回调
                pipeline.fireChannelReadComplete();
                //下面都是关闭连接的一些相关操作，这里就先不详细展开了
                if (exception != null) {
                    closed = closeOnReadError(exception);

                    pipeline.fireExceptionCaught(exception);
                }
                if (closed) {
                    inputShutdown = true;
                    if (isOpen()) {
                        close(voidPromise());
                    }
                }
            } finally {
                if (!readPending && !config.isAutoRead()) {
                    removeReadOp();
                }
            }
        }
    }

    protected boolean closeOnReadError(Throwable cause) {
        if (!isActive()) {
            return true;
        }
        if (cause instanceof PortUnreachableException) {
            return false;
        }
        if (cause instanceof IOException) {
            return !(this instanceof ServerChannel);
        }
        return true;
    }

    protected abstract int doReadMessages(List<Object> buf) throws Exception;

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        final SelectionKey key = selectionKey();
        final int interestOps = key.interestOps();

        for (;;) {
            Object msg = in.current();
            if (msg == null) {
                // Wrote all messages.
                if ((interestOps & SelectionKey.OP_WRITE) != 0) {
                    key.interestOps(interestOps & ~SelectionKey.OP_WRITE);
                }
                break;
            }
            try {
                boolean done = false;
                for (int i = config().getWriteSpinCount() - 1; i >= 0; i--) {
                    if (doWriteMessage(msg, in)) {
                        done = true;
                        break;
                    }
                }

                if (done) {
                    in.remove();
                } else {
                    if ((interestOps & SelectionKey.OP_WRITE) == 0) {
                        key.interestOps(interestOps | SelectionKey.OP_WRITE);
                    }
                    break;
                }
            } catch (Exception e) {
                if (continueOnWriteError()) {
                    in.remove(e);
                } else {
                    throw e;
                }
            }
        }
    }


    protected boolean continueOnWriteError() {
        return false;
    }

    protected abstract boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception;
}
