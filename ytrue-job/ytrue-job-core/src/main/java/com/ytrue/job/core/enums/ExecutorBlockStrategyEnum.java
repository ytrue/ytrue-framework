package com.ytrue.job.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ytrue
 * @date 2023-08-28 11:25
 * @description 阻塞处理策略
 */
@AllArgsConstructor
@Getter
public enum ExecutorBlockStrategyEnum {

    /**
     * 串行
     */
    SERIAL_EXECUTION("Serial execution"),

    /**
     * 直接丢弃
     */
    DISCARD_LATER("Discard Later"),
    /**
     * 覆盖
     */
    COVER_EARLY("Cover Early");

    private String title;

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 过去的对应的枚举
     *
     * @param name
     * @param defaultItem
     * @return
     */
    public static ExecutorBlockStrategyEnum match(String name, ExecutorBlockStrategyEnum defaultItem) {
        if (name != null) {
            for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
