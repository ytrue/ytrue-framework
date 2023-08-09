package com.ytrue.netty.util.internal;

import com.ytrue.netty.util.concurrent.EventExecutor;
import com.ytrue.netty.util.concurrent.FastThreadLocal;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author ytrue
 * @date 2023-08-09 14:40
 * @description ThreadExecutorMap
 */
public final class ThreadExecutorMap {

    private static final FastThreadLocal<EventExecutor> mappings = new FastThreadLocal<EventExecutor>();

    private ThreadExecutorMap() { }


    public static EventExecutor currentExecutor() {
        return mappings.get();
    }


    private static void setCurrentEventExecutor(EventExecutor executor) {
        mappings.set(executor);
    }


    public static Executor apply(final Executor executor, final EventExecutor eventExecutor) {
        ObjectUtil.checkNotNull(executor, "executor");
        ObjectUtil.checkNotNull(eventExecutor, "eventExecutor");
        return new Executor() {
            @Override
            public void execute(final Runnable command) {
                executor.execute(apply(command, eventExecutor));
            }
        };
    }


    public static Runnable apply(final Runnable command, final EventExecutor eventExecutor) {
        ObjectUtil.checkNotNull(command, "command");
        ObjectUtil.checkNotNull(eventExecutor, "eventExecutor");
        return new Runnable() {
            @Override
            public void run() {
                setCurrentEventExecutor(eventExecutor);
                try {
                    command.run();
                } finally {
                    setCurrentEventExecutor(null);
                }
            }
        };
    }


    public static ThreadFactory apply(final ThreadFactory threadFactory, final EventExecutor eventExecutor) {
        ObjectUtil.checkNotNull(threadFactory, "command");
        ObjectUtil.checkNotNull(eventExecutor, "eventExecutor");
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return threadFactory.newThread(apply(r, eventExecutor));
            }
        };
    }
}

