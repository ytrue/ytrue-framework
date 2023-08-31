package com.ytrue.job.admin.core.route.strategy;

import com.ytrue.job.admin.core.route.ExecutorRouter;
import com.ytrue.job.admin.core.scheduler.XxlJobScheduler;
import com.ytrue.job.admin.core.util.I18nUtil;
import com.ytrue.job.core.biz.ExecutorBiz;
import com.ytrue.job.core.biz.model.IdleBeatParam;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-31 9:34
 * @description 忙碌转移策略
 */
public class ExecutorRouteBusyover extends ExecutorRouter {
    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        StringBuffer idleBeatResultSB = new StringBuffer();
        //遍历执行器地址
        for (String address : addressList) {
            ReturnT<String> idleBeatResult;
            try {
                //得到向执行器发送消息的客户端
                ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
                //向客户端发送忙碌检测请求，判断该执行器的定时任务线程是否正在执行对应的定时任务
                //如果正在执行，说明比较忙碌，就不使用该地址
                idleBeatResult = executorBiz.idleBeat(new IdleBeatParam(triggerParam.getJobId()));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                idleBeatResult = new ReturnT<>(ReturnT.FAIL_CODE, "" + e);
            }
            idleBeatResultSB.append((idleBeatResultSB.length() > 0) ? "<br><br>" : "")
                    .append(I18nUtil.getString("jobconf_idleBeat") + "：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(idleBeatResult.getCode())
                    .append("<br>msg：").append(idleBeatResult.getMsg());
            //如果不忙碌就直接使用该地址
            if (idleBeatResult.getCode() == ReturnT.SUCCESS_CODE) {
                idleBeatResult.setMsg(idleBeatResultSB.toString());
                idleBeatResult.setContent(address);
                return idleBeatResult;
            }
        }
        return new ReturnT<>(ReturnT.FAIL_CODE, idleBeatResultSB.toString());
    }
}
