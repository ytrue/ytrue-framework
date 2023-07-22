package com.ytrue.netty.channel.nio;

import com.ytrue.netty.channel.EventLoopTaskQueueFactory;
import com.ytrue.netty.channel.SingleThreadEventLoop;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * @author ytrue
 * @date 2023-07-22 14:23
 * @description NioEventLoop
 */
@Slf4j
public class NioEventLoop extends SingleThreadEventLoop {


    private final ServerSocketChannel serverSocketChannel;

    private final SocketChannel socketChannel;

    /**
     * @Author: PP-jessica
     * @Description:这个属性是暂时的，后面我们会把它从该类中剔除
     */
    private NioEventLoop worker;

    public void setWorker(NioEventLoop worker) {
        this.worker = worker;
    }

    private final Selector selector;

    private final SelectorProvider provider;


    /**
     * @param serverSocketChannel 服务端ServerSocketChannel
     * @param socketChannel       客户端SocketChannel
     */
    public NioEventLoop(ServerSocketChannel serverSocketChannel, SocketChannel socketChannel) {
        this(null, SelectorProvider.provider(), null, serverSocketChannel, socketChannel);
    }


    /**
     * @param executor            执行器
     * @param selectorProvider    selectorProvider
     * @param queueFactory        队列工厂
     * @param serverSocketChannel 服务端ServerSocketChannel
     * @param socketChannel       客户端SocketChannel
     */
    public NioEventLoop(Executor executor, SelectorProvider selectorProvider, EventLoopTaskQueueFactory queueFactory, ServerSocketChannel serverSocketChannel, SocketChannel socketChannel) {

        super(executor, queueFactory);

        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        if (serverSocketChannel != null && socketChannel != null) {
            throw new RuntimeException("only one channel can be here! server or client!");
        }

        this.provider = selectorProvider;
        this.serverSocketChannel = serverSocketChannel;
        this.socketChannel = socketChannel;
        this.selector = openSelector();
    }


    public Selector unwrappedSelector() {
        return this.selector;
    }

    @Override
    protected void run() {
        while (true) {
            try {
                select();
                processSelectedKeys();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                runAllTasks();
            }
        }
    }


    private void select() throws IOException {
        Selector selector = this.selector;
        //这里是一个死循环
        for (; ; ) {
            //如果没有就绪事件，就在这里阻塞3秒
            int selectedKeys = selector.select(3000);
            //如果有事件或者单线程执行器中有任务待执行，就退出循环
            if (selectedKeys != 0 || hasTasks()) {
                break;
            }
        }
    }

    private void processSelectedKeys() throws Exception {
        //采用优化过后的方式处理事件,Netty默认会采用优化过的Selector对就绪事件处理。
        //processSelectedKeysOptimized();
        //未优化过的处理事件方式
        processSelectedKeysPlain(selector.selectedKeys());
    }

    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) throws Exception {
        if (selectedKeys.isEmpty()) {
            return;
        }
        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (; ; ) {
            final SelectionKey k = i.next();
            i.remove();
            //处理就绪事件
            processSelectedKey(k);
            if (!i.hasNext()) {
                break;
            }
        }
    }


    private void processSelectedKey(SelectionKey k) throws Exception {

        //说明传进来的是客户端channel，要处理客户端的事件
        if (socketChannel != null) {
            if (k.isConnectable()) {
                //channel已经连接成功
                if (socketChannel.finishConnect()) {
                    //注册读事件
                    socketChannel.register(selector, SelectionKey.OP_READ);
                }
            }

            //如果是读事件
            if (k.isReadable()) {
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                int len = socketChannel.read(byteBuffer);
                byte[] buffer = new byte[len];
                byteBuffer.flip();
                byteBuffer.get(buffer);
                log.info("客户端收到消息:{}", new String(buffer));
            }
            return;
        }


        //运行到这里说明是服务端的channel
        if (serverSocketChannel != null) {

            //连接事件
            if (k.isAcceptable()) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                //由worker执行器去执行注册
                worker.registerRead(socketChannel, worker);
                socketChannel.write(ByteBuffer.wrap("我还不是netty，但我知道你上线了".getBytes()));
                log.info("服务器发送消息成功！");
            }

            //如果是读事件
            if (k.isReadable()) {
                SocketChannel channel = (SocketChannel) k.channel();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                int len = channel.read(byteBuffer);
                if (len == -1) {
                    log.info("客户端通道要关闭！");
                    channel.close();
                    return;
                }
                byte[] bytes = new byte[len];
                byteBuffer.flip();
                byteBuffer.get(bytes);
                log.info("收到客户端发送的数据:{}", new String(bytes));
            }
        }
    }


    /**
     * 创建选择器
     *
     * @return
     */
    private Selector openSelector() {
        //未包装过的选择器
        final Selector unwrappedSelector;

        try {
            unwrappedSelector = provider.openSelector();
            return unwrappedSelector;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
