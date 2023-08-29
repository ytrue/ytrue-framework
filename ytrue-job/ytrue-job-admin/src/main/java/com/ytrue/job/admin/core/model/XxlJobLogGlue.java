package com.ytrue.job.admin.core.model;

import lombok.Data;

import java.util.Date;

/**
 * @author ytrue
 * @date 2023-08-29 9:44
 * @description glue方式的定时任务的日志
 */
@Data
public class XxlJobLogGlue {

	private int id;
	//定时任务id
	private int jobId;
	//glue的类型
	private String glueType;
	//glue源码
	private String glueSource;
	//glue备注
	private String glueRemark;
	private Date addTime;
	private Date updateTime;



}
