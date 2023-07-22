package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-22 13:52
 * @description 拒绝策略接口
 */
public interface RejectedExecutionHandler {

    void rejected(Runnable task, SingleThreadEventExecutor executor);
}
