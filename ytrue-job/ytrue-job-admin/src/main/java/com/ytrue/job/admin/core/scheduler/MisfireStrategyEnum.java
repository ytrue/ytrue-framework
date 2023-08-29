package com.ytrue.job.admin.core.scheduler;

import com.ytrue.job.admin.core.util.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ytrue
 * @date 2023-08-29 14:36
 * @description MisfireStrategyEnum
 */
@AllArgsConstructor
@Getter
public enum MisfireStrategyEnum {

    //默认什么也不做
    DO_NOTHING(I18nUtil.getString("misfire_strategy_do_nothing")),

    //失败后重试一次
    FIRE_ONCE_NOW(I18nUtil.getString("misfire_strategy_fire_once_now"));

    private final String title;


    public static MisfireStrategyEnum match(String name, MisfireStrategyEnum defaultItem) {
        for (MisfireStrategyEnum item : MisfireStrategyEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }
}
