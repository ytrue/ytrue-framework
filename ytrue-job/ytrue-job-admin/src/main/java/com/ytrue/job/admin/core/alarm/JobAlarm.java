package com.ytrue.job.admin.core.alarm;

import com.ytrue.job.admin.core.model.XxlJobInfo;
import com.ytrue.job.admin.core.model.XxlJobLog;

/**
 * @author ytrue
 * @date 2023-09-03 9:44
 * @description JobAlarm
 */
public interface JobAlarm {

    boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog);
}
