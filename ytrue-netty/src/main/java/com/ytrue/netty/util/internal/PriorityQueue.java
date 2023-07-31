package com.ytrue.netty.util.internal;

import java.util.Queue;

/**
 * @author ytrue
 * @date 2023-07-31 9:55
 * @description 定时任务队列的接口
 */
public interface PriorityQueue<T> extends Queue<T> {

    /**
     * 删除对象
     *
     * @param node
     * @return
     */
    boolean removeTyped(T node);

    /**
     * 队列是否包含该对象
     *
     * @param node
     * @return
     */
    boolean containsTyped(T node);


    void priorityChanged(T node);


    /**
     * 只把size置为0，但是数组中的定时任务还未被删除，相当于逻辑删除
     */
    void clearIgnoringIndexes();
}
