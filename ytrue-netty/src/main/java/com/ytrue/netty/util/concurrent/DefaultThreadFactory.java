package com.ytrue.netty.util.concurrent;

import com.ytrue.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ytrue
 * @date 2023-07-22 13:53
 * @description 默认的线程工厂
 */
@Slf4j
public class DefaultThreadFactory implements ThreadFactory {

    /**
     * id
     */
    private static final AtomicInteger POOL_ID = new AtomicInteger();

    /**
     * 下一个
     */
    private final AtomicInteger nextId = new AtomicInteger();

    /**
     * 前缀
     */
    private final String prefix;

    /**
     * 是否守护线程
     */
    private final boolean daemon;

    /**
     * 线程优先级
     */
    private final int priority;

    /**
     * 线程组
     */
    protected final ThreadGroup threadGroup;


    public DefaultThreadFactory() {
        this(DefaultThreadFactory.class);
    }

    public DefaultThreadFactory(Class<?> poolType) {
        //设置为非守护线程，优先级为5
        this(poolType, false, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(Class<?> poolType, boolean daemon, int priority) {
        this(toPoolName(poolType), daemon, priority);
    }

    public DefaultThreadFactory(String poolName, boolean daemon, int priority) {
        this(poolName, daemon, priority, System.getSecurityManager() == null ?
                Thread.currentThread().getThreadGroup() : System.getSecurityManager().getThreadGroup());
    }

    public DefaultThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
        if (poolName == null) {
            throw new NullPointerException("poolName");
        }
        //校验线程优先级
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "priority: " + priority + " (expected: Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY)");
        }
        //给属性赋值
        prefix = poolName + '-' + POOL_ID.incrementAndGet() + '-';
        this.daemon = daemon;
        this.priority = priority;
        this.threadGroup = threadGroup;
    }

    /**
     * 得到线程池的名字
     *
     * @param poolType
     * @return
     */
    public static String toPoolName(Class<?> poolType) {
        if (poolType == null) {
            throw new NullPointerException("poolType");
        }

        // 获取类名
        String poolName = StringUtil.simpleClassName(poolType);
        switch (poolName.length()) {
            case 0:
                return "unknown";
            case 1:
                return poolName.toLowerCase(Locale.US);
            default:
                if (Character.isUpperCase(poolName.charAt(0)) && Character.isLowerCase(poolName.charAt(1))) {
                    return Character.toLowerCase(poolName.charAt(0)) + poolName.substring(1);
                } else {
                    return poolName;
                }
        }
    }


    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread( r,prefix + nextId.incrementAndGet());
        log.info(t.toString());
        try {
            if (t.isDaemon() != daemon) {
                t.setDaemon(daemon);
            }

            if (t.getPriority() != priority) {
                t.setPriority(priority);
            }
        } catch (Exception exception) {
            log.error("创建线程失败:{}",exception.getMessage());
        }
        return t;
    }
}
