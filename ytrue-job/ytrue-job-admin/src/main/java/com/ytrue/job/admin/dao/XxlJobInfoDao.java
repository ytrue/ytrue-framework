package com.ytrue.job.admin.dao;

import com.ytrue.job.admin.core.model.XxlJobInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-29 14:29
 * @description XxlJobInfoDao
 */
@Mapper
public interface XxlJobInfoDao {


    List<XxlJobInfo> pageList(@Param("offset") int offset,
                              @Param("pagesize") int pagesize,
                              @Param("jobGroup") int jobGroup,
                              @Param("triggerStatus") int triggerStatus,
                              @Param("jobDesc") String jobDesc,
                              @Param("executorHandler") String executorHandler,
                              @Param("author") String author);


    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("jobGroup") int jobGroup,
                      @Param("triggerStatus") int triggerStatus,
                      @Param("jobDesc") String jobDesc,
                      @Param("executorHandler") String executorHandler,
                      @Param("author") String author);


    List<XxlJobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize);


    int scheduleUpdate(XxlJobInfo xxlJobInfo);

    XxlJobInfo loadById(@Param("id") int id);

    int save(XxlJobInfo info);

    int update(XxlJobInfo xxlJobInfo);

    int delete(@Param("id") long id);

    int findAllCount();

    List<XxlJobInfo> getJobsByGroup(@Param("jobGroup") int jobGroup);

}
