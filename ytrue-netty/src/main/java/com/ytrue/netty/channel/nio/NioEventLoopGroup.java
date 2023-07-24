package com.ytrue.netty.channel.nio;

import com.ytrue.netty.channel.*;
import com.ytrue.netty.util.concurrent.EventExecutorChooserFactory;
import com.ytrue.netty.util.concurrent.RejectedExecutionHandler;
import com.ytrue.netty.util.concurrent.RejectedExecutionHandlers;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author ytrue
 * @date 2023-07-24 9:56
 * @description NioEventLoopGroup
 */
public class NioEventLoopGroup extends MultiThreadEventLoopGroup {

    public NioEventLoopGroup() {
        this(0);
    }

    /**
     * @param nThreads 线程数量
     */
    public NioEventLoopGroup(int nThreads) {
        this(nThreads, (Executor) null);
    }

    /**
     * @param nThreads      线程数量
     * @param threadFactory 线程工厂
     */
    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory, SelectorProvider.provider());
    }

    /**
     * @param nThreads 线程数量
     * @param executor 线程池执行器
     */
    public NioEventLoopGroup(int nThreads, Executor executor) {
        this(nThreads, executor, SelectorProvider.provider());
    }


    /**
     * @param nThreads         线程数量
     * @param threadFactory    线程工厂
     * @param selectorProvider NIO SelectorProvider
     */
    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory, final SelectorProvider selectorProvider) {
        this(nThreads, threadFactory, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
    }

    /**
     * @param nThreads              线程数量
     * @param threadFactory         线程工厂
     * @param selectorProvider      NIO SelectorProvider
     * @param selectStrategyFactory selectStrategy工厂
     */
    public NioEventLoopGroup(
            int nThreads,
            ThreadFactory threadFactory,
            final SelectorProvider selectorProvider,
            final SelectStrategyFactory selectStrategyFactory
    ) {
        // 最终调用的是MultiThreadEventExecutorGroup#(nThreads,threadFactory,Object... args)
        super(nThreads, threadFactory, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }

    /**
     * @param nThreads         线程数量
     * @param executor         线程执行器
     * @param selectorProvider NIO SelectorProvider
     */
    public NioEventLoopGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider) {
        this(nThreads, executor, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
    }

    /**
     * @param nThreads
     * @param executor
     * @param selectorProvider
     * @param selectStrategyFactory
     */
    public NioEventLoopGroup(
            int nThreads,
            Executor executor,
            final SelectorProvider selectorProvider,
            final SelectStrategyFactory selectStrategyFactory
    ) {
        // 最终调用的是MultiThreadEventExecutorGroup#(nThreads,executor,Object... args)
        super(nThreads, executor, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }

    public NioEventLoopGroup(
            int nThreads,
            Executor executor,
            EventExecutorChooserFactory chooserFactory,
            final SelectorProvider selectorProvider,
            final SelectStrategyFactory selectStrategyFactory
    ) {
        // 最终调用的是MultiThreadEventExecutorGroup最全的那个
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }

    public NioEventLoopGroup(
            int nThreads,
            Executor executor,
            EventExecutorChooserFactory chooserFactory,
            final SelectorProvider selectorProvider,
            final SelectStrategyFactory selectStrategyFactory,
            final RejectedExecutionHandler rejectedExecutionHandler
    ) {
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory, rejectedExecutionHandler);
    }

    public NioEventLoopGroup(
            int nThreads,
            Executor executor,
            EventExecutorChooserFactory chooserFactory,
            final SelectorProvider selectorProvider,
            final SelectStrategyFactory selectStrategyFactory,
            final RejectedExecutionHandler rejectedExecutionHandler,
            final EventLoopTaskQueueFactory taskQueueFactory
    ) {
        // 最终调用的是MultiThreadEventExecutorGroup最全的那个
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory,
                rejectedExecutionHandler, taskQueueFactory);
    }

    @Override
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        // 如果可变参数等于4，拿下标为3的，其实就是 本类119行的这个构造
        EventLoopTaskQueueFactory queueFactory = args.length == 4 ? (EventLoopTaskQueueFactory) args[3] : null;

        // 创建nioLoop
        return new NioEventLoop(
                this,
                executor,
                (SelectorProvider) args[0],
                ((SelectStrategyFactory) args[1]).newSelectStrategy(),
                (RejectedExecutionHandler) args[2],
                queueFactory
        );
    }
}
