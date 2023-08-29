package com.ytrue.job.admin.core.trigger;

import com.ytrue.job.admin.core.util.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ytrue
 * @date 2023-08-29 14:39
 * @description TriggerTypeEnum
 */
@AllArgsConstructor
@Getter
public enum TriggerTypeEnum {
    // 手动触发
    MANUAL(I18nUtil.getString("jobconf_trigger_type_manual")),
    // Cron触发
    CRON(I18nUtil.getString("jobconf_trigger_type_cron")),
    // 失败重试触发
    RETRY(I18nUtil.getString("jobconf_trigger_type_retry")),
    //父任务触发
    PARENT(I18nUtil.getString("jobconf_trigger_type_parent")),
    // API触发
    API(I18nUtil.getString("jobconf_trigger_type_api")),
    //调度过期补偿
    MISFIRE(I18nUtil.getString("jobconf_trigger_type_misfire"));

    private final String title;

}
