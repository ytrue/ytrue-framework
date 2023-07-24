package com.ytrue.netty.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-07-24 9:04
 * @description 循环组的接口, 暂时先不继承ScheduledExecutorService接口了
 */
public interface EventExecutorGroup extends Executor {

    /**
     * 获取一个EventExecutor
     *
     * @return
     */
    EventExecutor next();

    /**
     * 优雅停机
     */
    void shutdownGracefully();


    /**
     * @author: ytrue
     * @description:下面这两个方法暂时不实现，源码中并不在本接口中，这里只是为了不报错，暂时放在这里
     */
    boolean isTerminated();

    void awaitTermination(Integer integer, TimeUnit timeUnit) throws InterruptedException;
}
