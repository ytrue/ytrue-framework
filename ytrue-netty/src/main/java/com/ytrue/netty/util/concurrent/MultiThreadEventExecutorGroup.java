package com.ytrue.netty.util.concurrent;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ytrue
 * @date 2023-07-24 9:22
 * @description MultiThreadEventExecutorGroup
 */
public abstract class MultiThreadEventExecutorGroup extends AbstractEventExecutorGroup {


    /**
     * 一组EventExecutor
     */
    private final EventExecutor[] children;

    /**
     * 只能读的一组EventExecutor
     */
    private final Set<EventExecutor> readonlyChildren;


    /**
     * 终止添加小孩
     */
    private final AtomicInteger terminatedChildren = new AtomicInteger();

    /**
     * 执行器
     */
    private final EventExecutorChooserFactory.EventExecutorChooser chooser;


    /**
     * @param nThreads      线程数量
     * @param threadFactory 线程工厂
     * @param args          参数
     */
    protected MultiThreadEventExecutorGroup(
            int nThreads,
            ThreadFactory threadFactory,
            Object... args
    ) {
        this(nThreads, threadFactory == null ? null : new ThreadPerTaskExecutor(threadFactory), args);
    }

    /**
     * @param nThreads 线程数量
     * @param executor 执行器
     * @param args     参数
     */
    protected MultiThreadEventExecutorGroup(
            int nThreads,
            Executor executor,
            Object... args
    ) {
        this(nThreads, executor, DefaultEventExecutorChooserFactory.INSTANCE, args);
    }


    /**
     * @param nThreads       线程树
     * @param executor       执行器
     * @param chooserFactory 选择器
     * @param args           参数
     */
    protected MultiThreadEventExecutorGroup(
            int nThreads,
            Executor executor,
            EventExecutorChooserFactory chooserFactory,
            Object... args
    ) {

        // 线程数量小于1抛异常
        if (nThreads <= 0) {
            throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
        }

        // 如果执行器为空创建默认的
        if (executor == null) {
            executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
        }

        // 在这里给线程组赋值，如果没有定义线程数，线程数默认就是cpu核数*2
        children = new EventExecutor[nThreads];

        for (int i = 0; i < nThreads; i++) {
            boolean success = false;
            try {
                //创建每一个线程执行器，这个方法在NioEventLoopGroup中实现。
                children[i] = newChild(executor, args);
                success = true;
            } catch (Exception e) {
                throw new IllegalStateException("failed to create a child event loop", e);
            } finally {
                if (!success) {
                    //如果第一个线程执行器就没创建成功，剩下的方法都不会执行
                    //如果从第二个线程执行器开始，执行器没有创建成功，那么就会关闭之前创建好的线程执行器。
                    for (int j = 0; j < i; j++) {
                        children[j].shutdownGracefully();
                    }

                    // 循环等待所有的执行器关闭完成
                    for (int j = 0; j < i; j++) {
                        EventExecutor e = children[j];
                        try {
                            //判断正在关闭的执行器的状态，如果还没终止，就等待一些时间再终止
                            while (!e.isTerminated()) {
                                e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                            }
                        } catch (InterruptedException interrupted) {
                            //给当前线程设置一个中断标志
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }

        //执行器选择器
        chooser = chooserFactory.newChooser(children);

        // 赋值不可变的set
        Set<EventExecutor> childrenSet = new LinkedHashSet<>(children.length);
        Collections.addAll(childrenSet, children);
        readonlyChildren = Collections.unmodifiableSet(childrenSet);
    }



    @Override
    public Iterator<EventExecutor> iterator() {
        return readonlyChildren.iterator();
    }

    @Override
    public EventExecutor next() {
        return chooser.next();
    }

    @Override
    public void shutdownGracefully() {
        // 依次关闭
        for (EventExecutor l : children) {
            l.shutdownGracefully();
        }
    }


    @Override
    @Deprecated
    public void shutdown() {
        for (EventExecutor l : children) {
            l.shutdown();
        }
    }

    @Override
    public boolean isShutdown() {
        for (EventExecutor l : children) {
            if (!l.isShutdown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTerminated() {
        for (EventExecutor l : children) {
            if (!l.isTerminated()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 等待 ExecutorService 中的所有任务完成，并在指定的超时时间内等待任务的完成。
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        loop:
        for (EventExecutor l : children) {
            for (; ; ) {
                long timeLeft = deadline - System.nanoTime();
                if (timeLeft <= 0) {
                    break loop;
                }
                if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
                    break;
                }
            }
        }
        return isTerminated();
    }


    /**
     * 执行器的总数
     *
     * @return
     */
    public final int executorCount() {
        return children.length;
    }


    /**
     * 创建线程工厂
     *
     * @return
     */
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass());
    }

    protected abstract EventExecutor newChild(Executor executor, Object... args) throws Exception;


}
