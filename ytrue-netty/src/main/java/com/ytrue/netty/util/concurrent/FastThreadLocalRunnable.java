package com.ytrue.netty.util.concurrent;

import com.ytrue.netty.util.internal.ObjectUtil;

/**
 * @author ytrue
 * @date 2023-08-03 11:11
 * @description 该类是对runable做了一个包装，目的是让使用FastThreadLocalThread的线程执行完毕后，
 * 可以自动删除FastThreadLocalMap中的数据
 */
final class FastThreadLocalRunnable implements Runnable {
    private final Runnable runnable;

    private FastThreadLocalRunnable(Runnable runnable) {
        this.runnable = ObjectUtil.checkNotNull(runnable, "runnable");
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } finally {
            //删除就体现在这里，线程推退出的时候肯定会执行该方法
            FastThreadLocal.removeAll();
        }
    }

    static Runnable wrap(Runnable runnable) {
        return runnable instanceof FastThreadLocalRunnable ? runnable : new FastThreadLocalRunnable(runnable);
    }
}
