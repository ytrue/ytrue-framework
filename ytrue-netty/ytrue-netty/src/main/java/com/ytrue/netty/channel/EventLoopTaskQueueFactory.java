package com.ytrue.netty.channel;

import java.util.Queue;

/**
 * @author ytrue
 * @date 2023-07-22 14:05
 * @description 创建任务队列的工厂
 */
public interface EventLoopTaskQueueFactory {

    Queue<Runnable> newTaskQueue(int maxCapacity);
}
