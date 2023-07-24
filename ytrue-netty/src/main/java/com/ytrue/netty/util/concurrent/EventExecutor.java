package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-24 9:04
 * @description EventLoop的接口，这个接口也继承了EventExecutorGroup，这样在EventLoopGroup中
 * EventLoop中就可以直接调用同名方法
 */
public interface EventExecutor extends EventExecutorGroup {

    /**
     * 获取EventExecutor
     * @return
     */
    @Override
    EventExecutor next();

    /**
     * 获取EventExecutorGroup
     *
     * @return
     */
    EventExecutorGroup parent();

    /**
     * 判断当前执行任务的线程是否是执行器的线程
     *
     * @param thread
     * @return
     */
    boolean inEventLoop(Thread thread);
}
