package com.ytrue.netty.channel;

import com.ytrue.netty.channel.nio.NioEventLoop;
import com.ytrue.netty.util.concurrent.DefaultThreadFactory;
import com.ytrue.netty.util.concurrent.SingleThreadEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author ytrue
 * @date 2023-07-22 14:18
 * @description 单线程事件循环，只要在netty中见到eventLoop，就可以把该类视为线程类
 */
@Slf4j
public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor {


    /**
     * @param executor     执行器
     * @param queueFactory 队列工厂
     */
    protected SingleThreadEventLoop(Executor executor, EventLoopTaskQueueFactory queueFactory) {
        super(executor, queueFactory, new DefaultThreadFactory());
    }

    protected SingleThreadEventLoop(Executor executor, EventLoopTaskQueueFactory queueFactory, ThreadFactory threadFactory) {
        super(executor, queueFactory, threadFactory);
    }


    /**
     * 是否有任务
     *
     * @return
     */
    protected boolean hasTasks() {
        return super.hasTasks();
    }


    /**
     * 注册socketChannel
     *
     * @param channel
     * @param nioEventLoop
     */
    public void register(SocketChannel channel, NioEventLoop nioEventLoop) {
        // 如果执行该方法的线程就是执行器中的线程，直接执行方法即可
        if (inEventLoop(Thread.currentThread())) {
            register0(channel, nioEventLoop);
        } else {
            // /在这里，第一次向单线程执行器中提交任务的时候，执行器终于开始执行了,新的线程也开始创建
            nioEventLoop.execute(() -> register0(channel, nioEventLoop));
            log.info("客户端的channel已注册到多路复用器上了！:{}", Thread.currentThread().getName());
        }
    }


    /**
     * 注册ServerSocketChannel
     *
     * @param channel
     * @param nioEventLoop
     */
    public void register(ServerSocketChannel channel, NioEventLoop nioEventLoop) {
        //如果执行该方法的线程就是执行器中的线程，直接执行方法即可
        if (inEventLoop(Thread.currentThread())) {
            register0(channel, nioEventLoop);
        } else {
            //在这里，第一次向单线程执行器中提交任务的时候，执行器终于开始执行了
            nioEventLoop.execute(() -> {
                register0(channel, nioEventLoop);
                log.info("服务器的channel已注册到多路复用器上了！:{}", Thread.currentThread().getName());
            });
        }
    }


    /**
     * 客户端实际注册
     *
     * @param channel
     * @param nioEventLoop
     */
    private void register0(SocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_CONNECT);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 服务端实际注册
     *
     * @param channel
     * @param nioEventLoop
     */
    private void register0(ServerSocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_ACCEPT);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }


    /**
     * 注册读
     *
     * @param channel
     * @param nioEventLoop
     */
    public void registerRead(SocketChannel channel, NioEventLoop nioEventLoop) {
        //如果执行该方法的线程就是执行器中的线程，直接执行方法即可
        if (inEventLoop(Thread.currentThread())) {
            register0(channel, nioEventLoop);
        } else {
            //在这里，第一次向单线程执行器中提交任务的时候，执行器终于开始执行了
            nioEventLoop.execute(() -> {
                register00(channel, nioEventLoop);
                log.info("客户端的channel已注册到多路复用器上了！:{}", Thread.currentThread().getName());
            });
        }
    }


    /**
     * 该方法是特意为服务端接收到客户端channel，然后将channel注册到sleector而重载的
     *
     * @param channel
     * @param nioEventLoop
     */
    private void register00(SocketChannel channel, NioEventLoop nioEventLoop) {
        try {
            channel.configureBlocking(false);
            channel.register(nioEventLoop.unwrappedSelector(), SelectionKey.OP_READ);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
