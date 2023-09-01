package com.ytrue.job.admin.dao;

import com.ytrue.job.admin.core.model.XxlJobLogReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-29 16:14
 * @description XxlJobLogReportDao
 */
@Mapper
public interface XxlJobLogReportDao {

    XxlJobLogReport queryLogReportTotal();

    List<XxlJobLogReport> queryLogReport(@Param("triggerDayFrom") Date triggerDayFrom,
                                         @Param("triggerDayTo") Date triggerDayTo);


    int save(XxlJobLogReport xxlJobLogReport);


    int update(XxlJobLogReport xxlJobLogReport);
}
