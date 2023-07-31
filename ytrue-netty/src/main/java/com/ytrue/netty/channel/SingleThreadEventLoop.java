package com.ytrue.netty.channel;

import com.ytrue.netty.util.concurrent.RejectedExecutionHandler;
import com.ytrue.netty.util.concurrent.SingleThreadEventExecutor;
import com.ytrue.netty.util.internal.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * @author ytrue
 * @date 2023-07-22 14:18
 * @description 单线程事件循环，只要在netty中见到eventLoop，就可以把该类视为线程类
 */
@Slf4j
public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop {


    /**
     * 任务队列的容量，默认是Integer的最大值
     */
    protected static final int DEFAULT_MAX_PENDING_TASKS = Integer.MAX_VALUE;


    protected SingleThreadEventLoop(
            EventLoopGroup parent,
            Executor executor,
            boolean addTaskWakesUp,
            Queue<Runnable> taskQueue,
            Queue<Runnable> tailTaskQueue,
            RejectedExecutionHandler rejectedExecutionHandler
    ) {
        super(parent, executor, addTaskWakesUp, taskQueue, rejectedExecutionHandler);
    }

    /**
     * @Author: ytrue
     * @Description:下面这两个方法会出现在这里，但并不是在这里实现的
     */
    @Override
    public EventLoopGroup parent() {
        return null;
    }

    @Override
    public EventLoop next() {
        return this;
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
     * 因为没有和ServerSocketChannel，SocketChannel解耦，这里原本是几个重载的注册方法。现在可以把这几个方法变成一个了
     *
     * @param channel
     * @return
     */
    @Override
    public ChannelFuture register(Channel channel) {
        //在这里可以发现在执行任务的时候，channel和promise也是绑定的
        return register(new DefaultChannelPromise(channel, this));
    }

    /**
     * 因为还没有引入unsafe类，所以该方法暂时先简化实现
     */
    @Override
    public ChannelFuture register(final ChannelPromise promise) {
        ObjectUtil.checkNotNull(promise, "promise");
        promise.channel().unsafe().register(this, promise);
        return promise;
    }
}
