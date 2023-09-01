package com.ytrue.job.admin.core.complete;

import com.ytrue.job.admin.core.conf.XxlJobAdminConfig;
import com.ytrue.job.admin.core.model.XxlJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ytrue
 * @date 2023-09-01 15:27
 * @description XxlJobCompleter
 */
public class XxlJobCompleter {

    private static Logger logger = LoggerFactory.getLogger(XxlJobCompleter.class);


    public static int updateHandleInfoAndFinish(XxlJobLog xxlJobLog) {
        //触发子任务的方法，这个版本用不上，就注释掉了
        //finishJob(xxlJobLog);

        //判断字符串长度
        if (xxlJobLog.getHandleMsg().length() > 15000) {
            //太长的话需要截取一段
            xxlJobLog.setHandleMsg(xxlJobLog.getHandleMsg().substring(0, 15000));
        }
        //更新数据库
        return XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateHandleInfo(xxlJobLog);
    }


}
