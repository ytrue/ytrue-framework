package com.ytrue.netty.demo03;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author ytrue
 * @date 2023-07-22 11:20
 * @description SingleThreadEventExecutor
 */
@Slf4j
public class SingleThreadEventExecutor implements Executor {

    /**
     * 队列最大值
     */
    private static final int DEFAULT_MAX_PENDING_TASKS = Integer.MAX_VALUE;


    /**
     * 任务队列
     */
    private final Queue<Runnable> taskQueue;

    /**
     * 队列满了的拒绝策略
     */
    private final RejectedExecutionHandler rejectedExecutionHandler;


    /**
     * 是否启动
     */
    private volatile boolean start;

    private SelectorProvider provider;

    private Selector selector;

    private Thread thread;


    /**
     * 默认构造
     */
    public SingleThreadEventExecutor() {
        provider = SelectorProvider.provider();
        taskQueue = newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
        rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();
        selector = openSelector();
    }

    /**
     * 创建队列
     *
     * @param number
     * @return
     */
    private Queue<Runnable> newTaskQueue(int number) {
        return new LinkedBlockingDeque<Runnable>(number);
    }

    /**
     * 创建选择器
     *
     * @return
     */
    private Selector openSelector() {
        try {
            return provider.openSelector();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 添加任务，并且执行
     *
     * @param task the runnable task
     */
    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        addTask(task);
        startThread();
    }

    /**
     * 启动线程
     */
    private void startThread() {
        if (start) {
            return;
        }
        start = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                thread = Thread.currentThread();
                SingleThreadEventExecutor.this.run();
            }
        });
        thread.start();
        log.info("新线程创建了");
    }


    /**
     * 添加任务
     *
     * @param task
     */
    private void addTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }
        if (offerTask(task)) {
            reject(task);
        }

    }

    /**
     * 拒绝策略
     *
     * @param task
     */
    private void reject(Runnable task) {
        // rejectedExecutionHandler.rejectedExecution(task,this);
    }

    /**
     * 添加元素到队列
     *
     * @param task
     * @return
     */
    private boolean offerTask(Runnable task) {
        //  添加一个元素并返回true 如果队列已满，则返回false
        return taskQueue.offer(task);
    }

    /**
     * 获取元素从队列
     *
     * @param taskQueue
     * @return
     */
    private static Runnable pollTaskFrom(Queue<Runnable> taskQueue) {
        //  移除并返问队列头部的元素    如果队列为空，则返回null
        return taskQueue.poll();
    }

    /**
     * 是否有任务
     *
     * @return
     */
    private boolean hasTask() {
        return !taskQueue.isEmpty();
    }

    /**
     * 执行队列所有任务
     */
    private void runAllTasks() {
        runAllTaskFrom(taskQueue);
    }

    /**
     * 执行对于队列的任务
     *
     * @param taskQueue
     */
    private void runAllTaskFrom(Queue<Runnable> taskQueue) {
        Runnable task = pollTaskFrom(taskQueue);
        if (task == null) {
            return;
        }

        while (true) {
            // 执行任务
            safeExecute(task);
            task = pollTaskFrom(taskQueue);
            if (task == null) {
                return;
            }
        }
    }

    /**
     * 执行任务
     *
     * @param task
     */
    private void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            log.warn("TASK RAISED AN Throwable Task {}", task, t);
        }
    }

    /**
     * 判断当前执行任务的线程是否是执行器的线程
     *
     * @param thread
     * @return
     */
    public boolean inEventLoop(Thread thread) {
        return thread == this.thread;
    }


    public void register(final SocketChannel socketChannel) {
        // 如果执行该方法的线程就是执行器中的线程，直接执行方法即可
        if (inEventLoop(Thread.currentThread())) {
            register0(socketChannel);
        } else {
            // /在这里，第一次向单线程执行器中提交任务的时候，执行器终于开始执行了,新的线程也开始创建
            this.execute(new Runnable() {
                @Override
                public void run() {
                    register0(socketChannel);
                }
            });
        }
    }

    private void register0(SocketChannel socketChannel) {
        try {
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    private void run() {
        while (true) {
            try {
                select();
                processSelectedKeys(selector.selectedKeys());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                runAllTasks();
            }
        }

    }

    private void select() throws IOException {
        Selector selector1 = this.selector;
        while (true) {
            int selectKeys = selector1.select(3000);
            if (selectKeys != 0 || hasTask()) {
                break;
            }
        }
    }

    private void processSelectedKeys(Set<SelectionKey> selectedKeys) {
        if (selectedKeys.isEmpty()) {
            return;
        }

        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            processSelectedKeys(key);
        }
    }

    private void processSelectedKeys(SelectionKey key) {
        if (key.isReadable()) {
            try {
                SocketChannel socketChannel = (SocketChannel) key.channel();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

                if (socketChannel.read(byteBuffer) == -1) {
                    //socketChannel.close();
                    key.cancel();
                }
                byteBuffer.flip();
                String str = Charset.forName("UTF-8").decode(byteBuffer).toString();
                log.info("我是服务端：获取客户端发送的数据内容：{}", str);
            } catch (Exception exception) {
                log.error(exception.getMessage());
                key.cancel();
            }
        }
    }

}
