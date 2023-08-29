package com.ytrue.job.admin.core.model;

import lombok.Data;

import java.util.Date;

/**
 * @author ytrue
 * @date 2023-08-29 9:44
 * @description 调度中心持有注册过来的执行器的实体类
 */
@Data
public class XxlJobRegistry {
    /**
     * 执行器id
     */
    private int id;
    /**
     * 执行器的注册方法，是手动还是自动
     */
    private String registryGroup;
    /**
     * 执行器的appName
     */
    private String registryKey;
    /**
     * 执行器的地址
     */
    private String registryValue;
    /**
     * 更新时间
     */
    private Date updateTime;
}
