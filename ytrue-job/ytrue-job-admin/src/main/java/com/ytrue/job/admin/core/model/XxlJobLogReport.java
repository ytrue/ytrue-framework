package com.ytrue.job.admin.core.model;

import lombok.Data;

import java.util.Date;


/**
 * @author ytrue
 * @date 2023-08-29 9:44
 * @description 日志报告对应的实体类
 */
@Data
public class XxlJobLogReport {
    private int id;
    private Date triggerDay;
    private int runningCount;
    private int sucCount;
    private int failCount;


}
