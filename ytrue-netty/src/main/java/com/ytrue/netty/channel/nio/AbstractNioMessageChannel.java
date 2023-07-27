package com.ytrue.netty.channel.nio;

import com.ytrue.netty.channel.Channel;

import java.nio.channels.SelectableChannel;
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
            boolean closed = false;
            Throwable exception = null;
            try {
                do {
                    //创建客户端的连接，存放在集合中
                    int localRead = doReadMessages(readBuf);
                    //返回值为0表示没有连接，直接退出即可
                    if (localRead == 0) {
                        break;
                    }
                } while (true);
            } catch (Throwable t) {
                exception = t;
            }
            int size = readBuf.size();
            for (int i = 0; i < size; i++) {
                readPending = false;
                //把每一个客户端的channel注册到工作线程上,这里得不到workgroup，所以我们不在这里实现了，打印一下即可
                Channel child = (Channel) readBuf.get(i);
                System.out.println(child + "收到客户端的channel了");
                //TODO
            }
            //清除集合
            readBuf.clear();
            if (exception != null) {
                throw new RuntimeException(exception);
            }
        }
    }

    protected abstract int doReadMessages(List<Object> buf) throws Exception;

    @Override
    protected void doWrite(Object msg) throws Exception {

    }
}
