package com.ytrue.job.admin.service;

import com.ytrue.job.admin.core.model.XxlJobInfo;
import com.ytrue.job.core.biz.model.ReturnT;

import java.util.Date;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-08-30 9:42
 * @description XxlJobService
 */
public interface XxlJobService {

    Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author);


    ReturnT<String> add(XxlJobInfo jobInfo);


    ReturnT<String> update(XxlJobInfo jobInfo);


    ReturnT<String> remove(int id);


    ReturnT<String> start(int id);


    ReturnT<String> stop(int id);


    Map<String, Object> dashboardInfo();


    ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate);
}
