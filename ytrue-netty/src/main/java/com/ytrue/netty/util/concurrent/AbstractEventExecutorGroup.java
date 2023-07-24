package com.ytrue.netty.util.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-07-24 9:19
 * @description 暂时还不实现任何方法，在源码中，这个抽象类定义了一些方法模版，都是对ScheduledExecutorService这个接口的方法的实现，
 */
public abstract class AbstractEventExecutorGroup implements EventExecutorGroup {

    @Override
    public void shutdownGracefully() {

    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public void awaitTermination(Integer integer, TimeUnit timeUnit) throws InterruptedException {

    }


    /**
     * 线程执行
     *
     * @param command the runnable task
     */
    @Override
    public void execute(Runnable command) {
        // 获取线程执行器执行方法
        next().execute(command);
    }
}
