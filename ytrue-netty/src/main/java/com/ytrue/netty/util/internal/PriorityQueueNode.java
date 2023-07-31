package com.ytrue.netty.util.internal;

/**
 * @author ytrue
 * @date 2023-07-31 9:53
 * @description 该接口的具体作用实际上只是记录了我们创建的定时任务在任务队列中的下标。具体实现可以看默认的实现类。这里再多说一句，netty
 * 的作者之所以搞这个接口，是为了减少寻找定时任务时，遍历队列的消耗。
 */
public interface PriorityQueueNode {

    /**
     * 如果一个任务不再队列中，把下标值设为-1，这里有点绕，看具体实现就会清晰很多
     */
    int INDEX_NOT_IN_QUEUE = -1;

    /**
     * 获取在传入队列中的下标地址
     *
     * @param queue
     * @return
     */
    int priorityQueueIndex(DefaultPriorityQueue<?> queue);

    /**
     * 设置当前的任务在队列中的下标位置
     *
     * @param queue
     * @param i
     */
    void priorityQueueIndex(DefaultPriorityQueue<?> queue, int i);
}
