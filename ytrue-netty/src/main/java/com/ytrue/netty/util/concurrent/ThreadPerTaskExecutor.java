package com.ytrue.netty.util.concurrent;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author ytrue
 * @date 2023-07-22 14:06
 * @description ThreadPerTaskExecutor
 */
@Slf4j
public class ThreadPerTaskExecutor implements Executor {

    private final ThreadFactory threadFactory;

    public ThreadPerTaskExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        this.threadFactory = threadFactory;
    }

    @Override
    public void execute(Runnable command) {
        //在这里创建线程并启动
        threadFactory.newThread(command).start();
        log.info("真正执行任务的线程被创建了！");
    }
}
