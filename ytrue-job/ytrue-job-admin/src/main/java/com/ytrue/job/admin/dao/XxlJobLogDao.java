package com.ytrue.job.admin.dao;

import com.ytrue.job.admin.core.model.XxlJobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-08-29 16:14
 * @description XxlJobLogDao
 */
@Mapper
public interface XxlJobLogDao {

    List<XxlJobLog> pageList(@Param("offset") int offset,
                             @Param("pagesize") int pagesize,
                             @Param("jobGroup") int jobGroup,
                             @Param("jobId") int jobId,
                             @Param("triggerTimeStart") Date triggerTimeStart,
                             @Param("triggerTimeEnd") Date triggerTimeEnd,
                             @Param("logStatus") int logStatus);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("jobGroup") int jobGroup,
                      @Param("jobId") int jobId,
                      @Param("triggerTimeStart") Date triggerTimeStart,
                      @Param("triggerTimeEnd") Date triggerTimeEnd,
                      @Param("logStatus") int logStatus);


    int delete(@Param("jobId") int jobId);


    List<Long> findClearLogIds(@Param("jobGroup") int jobGroup,
                               @Param("jobId") int jobId,
                               @Param("clearBeforeTime") Date clearBeforeTime,
                               @Param("clearBeforeNum") int clearBeforeNum,
                               @Param("pagesize") int pagesize);

    int clearLog(@Param("logIds") List<Long> logIds);

    XxlJobLog load(@Param("id") long id);

    long save(XxlJobLog xxlJobLog);

    int updateTriggerInfo(XxlJobLog xxlJobLog);

    List<Long> findLostJobIds(@Param("losedTime") Date losedTime);

    int updateHandleInfo(XxlJobLog xxlJobLog);

    Map<String, Object> findLogReport(@Param("from") Date from,
                                      @Param("to") Date to);


}
