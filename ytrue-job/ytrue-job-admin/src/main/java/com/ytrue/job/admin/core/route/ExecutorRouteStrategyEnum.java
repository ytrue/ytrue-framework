package com.ytrue.job.admin.core.route;

import com.ytrue.job.admin.core.route.strategy.ExecutorRouteFirst;
import com.ytrue.job.admin.core.util.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ytrue
 * @date 2023-08-30 10:00
 * @description 路由策略枚举类，第一版本中，我们只保留一个枚举对象，仅仅是为了代码不报错
 * 现在还用不到路由策略
 */
@AllArgsConstructor
@Getter
public enum ExecutorRouteStrategyEnum {

    FIRST(I18nUtil.getString("jobconf_route_first"), new ExecutorRouteFirst());

    private final String title;
    private final ExecutorRouter router;

    public static ExecutorRouteStrategyEnum match(String name, ExecutorRouteStrategyEnum defaultItem) {
        if (name != null) {
            for (ExecutorRouteStrategyEnum item : ExecutorRouteStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }

}
