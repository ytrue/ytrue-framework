package com.ytrue.netty.channel;

import com.ytrue.netty.util.concurrent.EventExecutor;

/**
 * 事件循环
 *
 * @author ytrue
 * @date 2023-07-24 9:08
 * @description EventLoop
 */
public interface EventLoop extends EventLoopGroup, EventExecutor {

    /**
     * 获取EventLoopGroup
     *
     * @return
     */
    @Override
    EventLoopGroup parent();
}
