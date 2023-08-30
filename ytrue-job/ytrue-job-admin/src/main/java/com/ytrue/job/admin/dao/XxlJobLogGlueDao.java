package com.ytrue.job.admin.dao;

import com.ytrue.job.admin.core.model.XxlJobLogGlue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-29 16:14
 * @description XxlJobLogGlueDao
 */
@Mapper
public interface XxlJobLogGlueDao {

    int deleteByJobId(@Param("jobId") int jobId);


    List<XxlJobLogGlue> findByJobId(@Param("jobId") int jobId);

    int save(XxlJobLogGlue xxlJobLogGlue);


    int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);
}
