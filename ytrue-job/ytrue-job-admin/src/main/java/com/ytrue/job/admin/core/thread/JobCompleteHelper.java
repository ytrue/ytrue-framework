package com.ytrue.job.admin.core.thread;

import com.ytrue.job.admin.core.complete.XxlJobCompleter;
import com.ytrue.job.admin.core.conf.XxlJobAdminConfig;
import com.ytrue.job.admin.core.model.XxlJobLog;
import com.ytrue.job.admin.core.util.I18nUtil;
import com.ytrue.job.core.biz.model.HandleCallbackParam;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author ytrue
 * @date 2023-09-01 11:16
 * @description 调度中心接收执行器回调信息的工作组件
 */
public class JobCompleteHelper {

    private static Logger logger = LoggerFactory.getLogger(JobCompleteHelper.class);

    /**
     * 单例对象
     */
    private static JobCompleteHelper instance = new JobCompleteHelper();

    public static JobCompleteHelper getInstance() {
        return instance;
    }

    /**
     * 回调线程池，这个线程池就是处理执行器端回调过来的日志信息的
     */
    private ThreadPoolExecutor callbackThreadPool = null;
    /**
     * 监控线程
     */
    private Thread monitorThread;
    private volatile boolean toStop = false;


    public void start() {
        //创建回调线程池
        callbackThreadPool = new ThreadPoolExecutor(2, 20, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(3000), r -> new Thread(r, "xxl-job, admin JobLosedMonitorHelper-callbackThreadPool-" + r.hashCode()), (r, executor) -> {
            r.run();
            logger.warn(">>>>>>>>>>> xxl-job, callback too fast, match threadpool rejected handler(run now).");
        });


        //创建监控线程
        monitorThread = new Thread(() -> {
            //这里休息了一会，是因为需要等待JobTriggerPoolHelper组件初始化，因为不执行远程调度，也就没有
            //回调过来的定时任务执行结果信息
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                if (!toStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            while (!toStop) {
                try {
                    //这里得到了一个时间信息，就是当前时间向前10分钟的时间
                    //这里传进去的参数-10，就是减10分钟的意思
                    Date losedTime = DateUtil.addMinutes(new Date(), -10);
                    //这里最后对应的就是这条sql语句
                    //t.trigger_code = 200 AND t.handle_code = 0 AND t.trigger_time <![CDATA[ <= ]]> #{losedTime} AND t2.id IS NULL
                    //其实就是判断了一下，现在在数据库中的xxljoblog的触发时间，其实就可以当作定时任务在调度中心开始执行的那个时间
                    //这里其实就是把当前时间前十分钟内提交执行的定时任务，但是始终没有得到执行器回调的执行结果的定时任务全找出来了
                    //因为t.handle_code = 0，并且注册表中也没有对应的数据了，说明心跳断了
                    //具体的方法在XxlJobLogMapper中
                    List<Long> losedJobIds = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findLostJobIds(losedTime);
                    if (losedJobIds != null && losedJobIds.size() > 0) {
                        //开始遍历定时任务
                        for (Long logId : losedJobIds) {
                            XxlJobLog jobLog = new XxlJobLog();
                            jobLog.setId(logId);
                            //设置执行时间
                            jobLog.setHandleTime(new Date());
                            //设置失败状态
                            jobLog.setHandleCode(ReturnT.FAIL_CODE);
                            jobLog.setHandleMsg(I18nUtil.getString("joblog_lost_fail"));
                            //更新失败的定时任务状态
                            XxlJobCompleter.updateHandleInfoAndFinish(jobLog);
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> xxl-job, job fail monitor thread error:{}", e);
                    }
                }
                try {
                    //每60秒工作一次
                    TimeUnit.SECONDS.sleep(60);
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> xxl-job, JobLosedMonitorHelper stop");
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("xxl-job, admin JobLosedMonitorHelper");
        monitorThread.start();
    }


    /**
     * 终止组件工作的方法
     */
    public void toStop() {
        toStop = true;
        callbackThreadPool.shutdownNow();
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }


    /**
     * 处理回调信息的方法
     *
     * @param callbackParamList
     * @return
     */
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        callbackThreadPool.execute(() -> {
            for (HandleCallbackParam handleCallbackParam : callbackParamList) {
                //在这里处理每一个回调的信息
                ReturnT<String> callbackResult = callback(handleCallbackParam);
                logger.debug(">>>>>>>>> JobApiController.callback {}, handleCallbackParam={}, callbackResult={}",
                        (callbackResult.getCode() == ReturnT.SUCCESS_CODE ? "success" : "fail"), handleCallbackParam, callbackResult);
            }
        });
        return ReturnT.SUCCESS;
    }


    /**
     * 真正处理回调信息的方法
     *
     * @param handleCallbackParam
     * @return
     */
    private ReturnT<String> callback(HandleCallbackParam handleCallbackParam) {
        //得到对应的xxljoblog对象
        XxlJobLog log = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().load(handleCallbackParam.getLogId());
        if (log == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "log item not found.");
        }
        //判断日志对象的处理结果码
        //因为这个响应码无论是哪种情况都是大于0的，如果大于0了，说明已经回调一次了
        //如果等于0，说明还没得到回调信息，任务也可能还处于运行中状态
        if (log.getHandleCode() > 0) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "log repeate callback.");
        }
        //拼接信息
        StringBuilder handleMsg = new StringBuilder();
        if (log.getHandleMsg() != null) {
            handleMsg.append(log.getHandleMsg()).append("<br>");
        }
        if (handleCallbackParam.getHandleMsg() != null) {
            handleMsg.append(handleCallbackParam.getHandleMsg());
        }
        // 设置结果
        log.setHandleTime(new Date());
        log.setHandleCode(handleCallbackParam.getHandleCode());
        log.setHandleMsg(handleMsg.toString());
        //更新数据库中的日志信息
        XxlJobCompleter.updateHandleInfoAndFinish(log);
        return ReturnT.SUCCESS;
    }

}
