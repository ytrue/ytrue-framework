package com.ytrue.netty.util.concurrent;

import java.util.Arrays;

/**
 * @author ytrue
 * @date 2023-07-25 9:43
 * @description 监听器的默认实现类，实际上该类只是对监听器进行了一层包装，内部持有一个监听器的数组，向promise添加的监听器最终都添加到该类的数组中了
 */
final class DefaultFutureListeners {

    private GenericFutureListener<? extends Future<?>>[] listeners;

    private int size;

    private int progressiveSize;

    /**
     * @param first  第一个
     * @param second 第二个
     */
    DefaultFutureListeners(GenericFutureListener<? extends Future<?>> first, GenericFutureListener<? extends Future<?>> second) {
        listeners = new GenericFutureListener[2];
        listeners[0] = first;
        listeners[1] = second;
        size = 2;
        if (first instanceof GenericProgressiveFutureListener) {
            progressiveSize++;
        }
        if (second instanceof GenericProgressiveFutureListener) {
            progressiveSize++;
        }
    }

    /**
     * 添加监听器
     *
     * @param l
     */
    public void add(GenericFutureListener<? extends Future<?>> l) {
        GenericFutureListener<? extends Future<?>>[] listeners = this.listeners;

        final int size = this.size;
        // 判断长度和size是否相对
        if (size == listeners.length) {
            // 扩容复制
            this.listeners = listeners = Arrays.copyOf(listeners, size << 1);
        }
        // 在尾部插入
        listeners[size] = l;
        // 数量加1
        this.size = size + 1;

        // 判断是不是GenericProgressiveFutureListener，是的话 progressiveSize + 1
        if (l instanceof GenericProgressiveFutureListener) {
            progressiveSize++;
        }
    }

    /**
     * 移除监听器
     *
     * @param l
     */
    public void remove(GenericFutureListener<? extends Future<?>> l) {
        final GenericFutureListener<? extends Future<?>>[] listeners = this.listeners;

        int size = this.size;

        for (int i = 0; i < size; i++) {
            // 如果地址相等
            if (listeners[i] == l) {
                int listenersToMove = size - i - 1;

                // 如果listenersToMove大于0
                if (listenersToMove > 0) {
                    System.arraycopy(listeners, i + 1, listeners, i, listenersToMove);
                }

                // 设置空，下次gc回收
                listeners[--size] = null;
                this.size = size;

                // 判断是不是GenericProgressiveFutureListener，是的话 progressiveSize - 1
                if (l instanceof GenericProgressiveFutureListener) {
                    progressiveSize--;
                }
                return;
            }
        }
    }

    public GenericFutureListener<? extends Future<?>>[] listeners() {
        return listeners;
    }

    public int size() {
        return size;
    }

    public int progressiveSize() {
        return progressiveSize;
    }
}
