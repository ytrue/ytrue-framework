package com.ytrue.job.core.handler;

/**
 * @author ytrue
 * @date 2023-08-28 11:33
 * @description 封装定时任务方法的接口
 */
public abstract class IJobHandler {


    public abstract void execute() throws Exception;


    public void init() throws Exception {

    }


    public void destroy() throws Exception {

    }
}
