package com.ytrue.netty.channel;

import com.ytrue.netty.util.NettyRuntime;
import com.ytrue.netty.util.concurrent.DefaultThreadFactory;
import com.ytrue.netty.util.concurrent.EventExecutorChooserFactory;
import com.ytrue.netty.util.concurrent.MultiThreadEventExecutorGroup;
import com.ytrue.netty.util.internal.SystemPropertyUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author ytrue
 * @date 2023-07-24 9:50
 * @description MultithreadEventLoopGroup
 */
public abstract class MultiThreadEventLoopGroup extends MultiThreadEventExecutorGroup implements EventLoopGroup {


    /**
     * 默认的线程数量
     */
    private static final int DEFAULT_EVENT_LOOP_THREADS;


    static {
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
    }


    /**
     * @param nThreads      线程数量
     * @param threadFactory 线程工厂
     * @param args          参数
     */
    protected MultiThreadEventLoopGroup(
            int nThreads,
            ThreadFactory threadFactory,
            Object... args
    ) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory, args);
    }

    /**
     * @param nThreads 线程数量
     * @param executor 执行器
     * @param args     参数
     */
    protected MultiThreadEventLoopGroup(
            int nThreads,
            Executor executor,
            Object... args
    ) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
    }

    /**
     * @param nThreads       线程树
     * @param executor       执行器
     * @param chooserFactory 选择器
     * @param args           参数
     */
    protected MultiThreadEventLoopGroup(
            int nThreads,
            Executor executor,
            EventExecutorChooserFactory chooserFactory,
            Object... args
    ) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, chooserFactory, args);
    }

    @Override
    protected abstract EventLoop newChild(Executor executor, Object... args) throws Exception;

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    @Override
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass(), Thread.MAX_PRIORITY);
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return next().register(channel);
    }

    @Override
    public ChannelFuture register(ChannelPromise promise) {
        return next().register(promise);
    }
}
