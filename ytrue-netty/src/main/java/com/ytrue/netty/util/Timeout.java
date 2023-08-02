package com.ytrue.netty.util;

/**
 * @author ytrue
 * @date 2023-08-02 9:10
 * @description 表示定时任务的接口，它提供了一些方法来操作和查询定时任务的状态
 */
public interface Timeout {

    /**
     * 获取关联的Timer对象，即创建该Timeout的定时器。
     * @return
     */
    Timer timer();


    /**
     * 获取关联的TimerTask对象，即要执行的定时任务
     * @return
     */
    TimerTask task();


    /**
     * 判断该定时任务是否已经过期，即是否已经到达执行时间。
     * @return
     */
    boolean isExpired();


    /**
     * 判断该定时任务是否已经被取消。
     * @return
     */
    boolean isCancelled();


    /**
     * 如果任务已经被执行或者已经取消，则返回false；否则返回true
     * @return
     */
    boolean cancel();
}
