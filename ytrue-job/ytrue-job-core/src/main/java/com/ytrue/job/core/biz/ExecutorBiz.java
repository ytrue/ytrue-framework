package com.ytrue.job.core.biz;

import com.ytrue.job.core.biz.model.*;

/**
 * @author ytrue
 * @date 2023-08-28 11:02
 * @description 用于远程调用的客户端接口，该接口中定义了多个方法，第一版本只保留一个
 */
public interface ExecutorBiz {

    /**
     * 远程调用的方法
     *
     * @param triggerParam
     * @return
     */
    ReturnT<String> run(TriggerParam triggerParam);


    /**
     * 心跳检测方法
     *
     * @return
     */
    ReturnT<String> beat();

    /**
     * 判断调度中心调度的定时任务是否在执行器对应的任务线程的队列中
     *
     * @param idleBeatParam
     * @return
     */
    ReturnT<String> idleBeat(IdleBeatParam idleBeatParam);


    /**
     *
     * @param logParam
     * @return
     */
    ReturnT<LogResult> log(LogParam logParam);

}
