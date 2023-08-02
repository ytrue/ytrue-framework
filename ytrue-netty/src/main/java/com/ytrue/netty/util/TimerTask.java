package com.ytrue.netty.util;

/**
 * @author ytrue
 * @date 2023-08-02 9:09
 * @description 定时执行任务接口
 */
public interface TimerTask {

    /**
     * 运行的方法
     *
     * @param timeout
     * @throws Exception
     */
    void run(Timeout timeout) throws Exception;
}
