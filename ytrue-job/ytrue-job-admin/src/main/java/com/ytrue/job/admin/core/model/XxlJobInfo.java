package com.ytrue.job.admin.core.model;

import lombok.Data;

import java.util.Date;

/**
 * @author ytrue
 * @date 2023-08-29 9:44
 * @description 定时任务的实体类，对应数据库中的xxl-job-info这张表
 */
@Data
public class XxlJobInfo {
    //定时任务id
    private int id;
    //该定时任务所属的执行器的id
    private int jobGroup;
    //定时任务描述
    private String jobDesc;
    //定时任务添加的时间
    private Date addTime;
    //定时任务的更新时间
    private Date updateTime;
    //负责人
    private String author;
    //报警邮件
    private String alarmEmail;
    //调度类型
    private String scheduleType;
    //一般为调度的cron表达式
    private String scheduleConf;
    //定时任务的失败策略
    private String misfireStrategy;
    //定时任务的路由策略
    private String executorRouteStrategy;
    //JobHandler的名称
    private String executorHandler;
    //执行器参数
    private String executorParam;
    //定时任务阻塞策略
    private String executorBlockStrategy;
    //执行超时时间
    private int executorTimeout;
    //失败重试次数
    private int executorFailRetryCount;
    //定时任务运行类型
    private String glueType;
    //glue的源码
    private String glueSource;
    //glue备注
    private String glueRemark;
    //glue更新时间
    private Date glueUpdatetime;
    //子任务id
    private String childJobId;
    //定时任务触发状态，0为停止，1为运行
    private int triggerStatus;
    //最近一次的触发时间
    private long triggerLastTime;
    //下一次的触发时间
    private long triggerNextTime;
}
