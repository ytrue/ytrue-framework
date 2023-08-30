package com.ytrue.job.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author ytrue
 * @date 2023-08-29 16:14
 * @description XxlJobLogDao
 */
@Mapper
public interface XxlJobLogDao {


    int delete(@Param("jobId") int jobId);
}
