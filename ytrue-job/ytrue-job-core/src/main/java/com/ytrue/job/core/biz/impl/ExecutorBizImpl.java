package com.ytrue.job.core.biz.impl;

import com.ytrue.job.core.biz.ExecutorBiz;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.biz.model.TriggerParam;
import com.ytrue.job.core.executor.XxlJobExecutor;
import com.ytrue.job.core.glue.GlueTypeEnum;
import com.ytrue.job.core.handler.IJobHandler;
import com.ytrue.job.core.thread.JobThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ytrue
 * @date 2023-08-28 14:32
 * @description ExecutorBizImpl
 */
public class ExecutorBizImpl implements ExecutorBiz {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorBizImpl.class);


    /**
     * 这个方法其实就是 获取对应的JobThread 之后向队列添加参数
     *
     * @param triggerParam
     * @return
     */
    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        //通过定时任务的ID从jobThreadRepository这个Map中获取一个具体的用来执行定时任务的线程
        JobThread jobThread = XxlJobExecutor.loadJobThread(triggerParam.getJobId());

        //判断该jobThread是否为空，不为空则说明该定时任务不是第一次执行了，也就意味着该线程已经分配了定时任务了，也就是这个jobHandler对象
        //如果为空，说明该定时任务是第一次执行，还没有分配jobThread
        IJobHandler jobHandler = jobThread != null ? jobThread.getHandler() : null;

        String removeOldReason = null;

        //得到定时任务的调度模式
        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());

        //如果为bean模式，就通过定时任务的名字，从jobHandlerRepository这个Map中获得jobHandler
        if (GlueTypeEnum.BEAN == glueTypeEnum) {
            //在这里获得定时任务对应的jobHandler对象，其实就是MethodJobHandler对象
            IJobHandler newJobHandler = XxlJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());

            //这里会进行一下判断，如果上面得到的jobHandler并不为空，说明该定时任务已经执行过了，并且分配了对应的执行任务的线程
            //但是根据定时任务的名字，从jobHandlerRepository这个Map中得到封装定时任务方法的对象却和jobHandler不相同
            //说明定时任务已经改变了
            if (jobThread != null && jobHandler != newJobHandler) {
                //走到这里就意味着定时任务已经改变了，要做出相应处理，需要把旧的线程杀死
                removeOldReason = "change jobhandler or glue type, and terminate the old job thread.";
                //执行定时任务的线程和封装定时任务方法的对象都置为null
                jobThread = null;
                jobHandler = null;
            }
            if (jobHandler == null) {
                //如果走到这里，就意味着jobHandler为null，这也就意味着上面得到的jobThread为null
                //这就说明，这次调度的定时任务是第一次执行，所以直接让jobHandler等于从jobHandlerRepository这个Map获得newJobHandler即可
                //然后，这个jobHandler会在下面创建JobThread的时候用到
                jobHandler = newJobHandler;
                if (jobHandler == null) {
                    //经过上面的赋值，
                    //走到这里如果jobHandler仍然为null，那只有一个原因，就是执行器这一端根本就没有对应的定时任务
                    //通过执行器的名字根本从jobHandlerRepository这个Map中找不到要被执行的定时任务
                    return new ReturnT<>(ReturnT.FAIL_CODE, "job handler [" + triggerParam.getExecutorHandler() + "] not found.");
                }
            }

        } else {
            //如果没有合适的调度模式，就返回调用失败的信息
            return new ReturnT<>(ReturnT.FAIL_CODE, "glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }

        if (jobThread == null) {
            //走到这里意味着定时任务是第一次执行，还没有创建对应的执行定时任务的线程，所以，就在这里把对应的线程创建出来，
            //并且缓存到jobThreadRepository这个Map中
            //在这里就用到了上面赋值过的jobHandler
            jobThread = XxlJobExecutor.registJobThread(triggerParam.getJobId(), jobHandler, removeOldReason);
        }
        //如果走到这里，不管上面是什么情况吧，总之jobThread肯定存在了，所以直接把要调度的任务放到这个线程内部的队列中
        //等待线程去调用，返回一个结果
        ReturnT<String> pushResult = jobThread.pushTriggerQueue(triggerParam);
        return pushResult;
    }
}
