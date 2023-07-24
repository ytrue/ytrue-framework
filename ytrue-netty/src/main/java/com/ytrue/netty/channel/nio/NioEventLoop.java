package com.ytrue.netty.channel.nio;

import com.ytrue.netty.channel.EventLoopGroup;
import com.ytrue.netty.channel.EventLoopTaskQueueFactory;
import com.ytrue.netty.channel.SelectStrategy;
import com.ytrue.netty.channel.SingleThreadEventLoop;
import com.ytrue.netty.util.concurrent.RejectedExecutionHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author ytrue
 * @date 2023-07-22 14:23
 * @description NioEventLoop
 */
@Slf4j
public class NioEventLoop extends SingleThreadEventLoop {


    /**
     * @Author: ytrue
     * @Description:这个属性是暂时的
     */
    @Setter
    private EventLoopGroup workerGroup;

    /**
     * 索引
     */
    private static int index = 0;

    /**
     * id值
     */
    private int id = 0;


    /**
     * 选择策略
     */
    private SelectStrategy selectStrategy;

    /**
     * 下面是与nio相关的组件
     */
    @Setter
    private ServerSocketChannel serverSocketChannel;

    @Setter
    private SocketChannel socketChannel;

    private final Selector selector;

    private final SelectorProvider provider;


    /**
     * @param parent                   父亲
     * @param executor                 执行器
     * @param selectorProvider         nio selectorProvider
     * @param strategy                 选择策略
     * @param rejectedExecutionHandler 拒绝策略
     * @param queueFactory             队列
     */
    NioEventLoop(
            NioEventLoopGroup parent,
            Executor executor,
            SelectorProvider selectorProvider,
            SelectStrategy strategy,
            RejectedExecutionHandler rejectedExecutionHandler,
            EventLoopTaskQueueFactory queueFactory
    ) {
        super(
                parent,
                executor,
                false,
                newTaskQueue(queueFactory),
                newTaskQueue(queueFactory),
                rejectedExecutionHandler
        );

        // 空校验
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        if (strategy == null) {
            throw new NullPointerException("selectStrategy");
        }

        provider = selectorProvider;
        selector = openSelector();
        selectStrategy = strategy;
        log.info("我是" + ++index + "NioEventLoop");
        id = index;
        log.info("work" + id);
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


    /**
     * 此时，应该也可以意识到，在不参考netty源码的情况下编写该方法，直接传入serverSocketChannel或者
     * socketChannel参数，每一次都要做几步判断，因为单线程的执行器是客户端和服务端通用的，所以你不知道传进来的参数究竟是
     * 什么类型的channel，那么复杂的判断就必不可少了，代码也就变得丑陋。这种情况，实际上应该想到完美的解决方法了，
     * 就是使用反射，传入Class，用工厂反射创建对象。netty中就是这么做的。
     */
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

                //注册客户端的channel到多路复用器，这里的操作是由服务器的单线程执行器执行的，在netty源码中，这里注册
                //客户端channel到多路复用器是由workGroup管理的线程执行器完成的。

                NioEventLoop nioEventLoop = (NioEventLoop) workerGroup.next().next();
                nioEventLoop.setServerSocketChannel(serverSocketChannel);
                log.info("+++++++++++++++++++++++++++++++++++++++++++要注册到第" + nioEventLoop.id + "work上！");
                //work线程自己注册的channel到执行器
                nioEventLoop.registerRead(socketChannel, nioEventLoop);


                socketChannel.write(ByteBuffer.wrap("我还不是netty，但我知道你上线了".getBytes()));
                log.info("服务器发送消息成功！");
            }

            //如果是读事件
            if (k.isReadable()) {
                SocketChannel channel = (SocketChannel) k.channel();
                try {
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
                } catch (Exception e) {
                    e.printStackTrace();
                    channel.close();
                }
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


    /**
     * 创建队列
     *
     * @param queueFactory
     * @return
     */
    private static Queue<Runnable> newTaskQueue(EventLoopTaskQueueFactory queueFactory) {
        if (queueFactory == null) {
            return new LinkedBlockingQueue<>(DEFAULT_MAX_PENDING_TASKS);
        }
        return queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
    }


}
