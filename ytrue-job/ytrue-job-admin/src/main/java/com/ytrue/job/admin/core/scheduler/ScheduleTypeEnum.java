package com.ytrue.job.admin.core.scheduler;

import com.ytrue.job.admin.core.util.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ytrue
 * @date 2023-08-29 14:48
 * @description ScheduleTypeEnum
 */
@AllArgsConstructor
@Getter
public enum ScheduleTypeEnum {


    //不使用任何类型
    NONE(I18nUtil.getString("schedule_type_none")),

    //一般都是用cron表达式
    CRON(I18nUtil.getString("schedule_type_cron")),

    //按照固定频率
    FIX_RATE(I18nUtil.getString("schedule_type_fix_rate"));


    private final String title;

    public static ScheduleTypeEnum match(String name, ScheduleTypeEnum defaultItem){
        for (ScheduleTypeEnum item: ScheduleTypeEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }
}
