package com.ytrue.netty.util.concurrent;

import com.ytrue.netty.util.internal.InternalThreadLocalMap;
import com.ytrue.netty.util.internal.UnstableApi;

/**
 * @author ytrue
 * @date 2023-08-03 11:12
 * @description 该线程和原生的thread其实没什么大的不同，Netty的线程可以对传进来的runnable包装一下，然后配合fastthreadlocal返回一个
 * InternalThreadLocalMap
 */
public class FastThreadLocalThread extends Thread {
    // This will be set to true if we have a chance to wrap the Runnable.
    private final boolean cleanupFastThreadLocals;

    private InternalThreadLocalMap threadLocalMap;

    public FastThreadLocalThread() {
        cleanupFastThreadLocals = false;
    }

    public FastThreadLocalThread(Runnable target) {
        super(FastThreadLocalRunnable.wrap(target));
        cleanupFastThreadLocals = true;
    }

    public FastThreadLocalThread(ThreadGroup group, Runnable target) {
        super(group, FastThreadLocalRunnable.wrap(target));
        cleanupFastThreadLocals = true;
    }

    public FastThreadLocalThread(String name) {
        super(name);
        cleanupFastThreadLocals = false;
    }

    public FastThreadLocalThread(ThreadGroup group, String name) {
        super(group, name);
        cleanupFastThreadLocals = false;
    }

    public FastThreadLocalThread(Runnable target, String name) {
        super(FastThreadLocalRunnable.wrap(target), name);
        cleanupFastThreadLocals = true;
    }

    public FastThreadLocalThread(ThreadGroup group, Runnable target, String name) {
        super(group, FastThreadLocalRunnable.wrap(target), name);
        cleanupFastThreadLocals = true;
    }

    public FastThreadLocalThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, FastThreadLocalRunnable.wrap(target), name, stackSize);
        cleanupFastThreadLocals = true;
    }


    public final InternalThreadLocalMap threadLocalMap() {
        return threadLocalMap;
    }


    public final void setThreadLocalMap(InternalThreadLocalMap threadLocalMap) {
        this.threadLocalMap = threadLocalMap;
    }

    @UnstableApi
    public boolean willCleanupFastThreadLocals() {
        return cleanupFastThreadLocals;
    }


    @UnstableApi
    public static boolean willCleanupFastThreadLocals(Thread thread) {
        return thread instanceof FastThreadLocalThread &&
                ((FastThreadLocalThread) thread).willCleanupFastThreadLocals();
    }
}

