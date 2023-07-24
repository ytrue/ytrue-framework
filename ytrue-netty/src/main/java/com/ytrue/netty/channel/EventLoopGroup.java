package com.ytrue.netty.channel;

import com.ytrue.netty.util.concurrent.EventExecutorGroup;

/**
 * @author ytrue
 * @date 2023-07-24 9:07
 * @description EventLoopGroup
 */
public interface EventLoopGroup extends EventExecutorGroup {

    /**
     * 获取EventLoop
     *
     * @return
     */
    @Override
    EventLoop next();
}
