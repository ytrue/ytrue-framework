package com.ytrue.netty.channel.nio;

import com.ytrue.netty.channel.EventLoopGroup;
import com.ytrue.netty.channel.EventLoopTaskQueueFactory;
import com.ytrue.netty.channel.SelectStrategy;
import com.ytrue.netty.channel.SingleThreadEventLoop;
import com.ytrue.netty.channel.socket.nio.NioSocketChannel;
import com.ytrue.netty.util.concurrent.RejectedExecutionHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.*;
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
     * 选择策略
     */
    private SelectStrategy selectStrategy;

    /**
     * 下面是与nio相关的组件
     */
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
            int selectedKeys = selector.select(1000);
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

            //还记得channel在注册时的第三个参数this吗？这里通过attachment方法就可以得到nio类的channel
            final Object a = k.attachment();

            i.remove();
            //处理就绪事件
            //处理就绪事件
            if (a instanceof AbstractNioChannel) {
                processSelectedKey(k, (AbstractNioChannel) a);
            }
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
    private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) throws Exception {
        try {
            //获取Unsafe类
            final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
            //得到key感兴趣的事件
            int ops = k.interestOps();
            //如果是连接事件
            if (ops == SelectionKey.OP_CONNECT) {
                //移除连接事件，否则会一直通知，这里实际上是做了个减法。位运算的门道，我们会放在之后和线程池的状态切换一起讲
                //这里先了解就行
                ops &= ~SelectionKey.OP_CONNECT;
                //重新把感兴趣的事件注册一下
                k.interestOps(ops);
                //然后再注册客户端channel感兴趣的读事件
                ch.doBeginRead();
                //这里要做真正的客户端连接处理
                unsafe.finishConnect();
            }
            //如果是读事件，不管是客户端还是服务端的，都可以直接调用read方法
            //这时候一定要记清楚，NioSocketChannel和NioServerSocketChannel并不会纠缠
            //用户创建的是哪个channel，这里抽象类调用就是它的方法
            //如果不明白，那么就找到AbstractNioChannel的方法看一看，想一想，虽然那里传入的参数是this，但传入的并不是抽象类本身，想想你创建的
            //是NioSocketChannel还是NioServerSocketChannel，是哪个，传入的就是哪个。只不过在这里被多态赋值给了抽象类
            //创建的是子类对象，但在父类中调用了this，得到的仍然是子类对象
            if (ops == SelectionKey.OP_READ) {
                unsafe.read();
            }
            if (ops == SelectionKey.OP_ACCEPT) {
                 unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            k.cancel();
            throw new RuntimeException(ignored.getMessage());
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
