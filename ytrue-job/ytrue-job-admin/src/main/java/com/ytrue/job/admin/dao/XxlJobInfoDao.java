package com.ytrue.job.admin.dao;

import com.ytrue.job.admin.core.model.XxlJobInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-29 14:29
 * @description XxlJobInfoDao
 */
public interface XxlJobInfoDao {


    List<XxlJobInfo> scheduleJobQuery(@Param("maxNextTime") long maxNextTime, @Param("pagesize") int pagesize);


    int scheduleUpdate(XxlJobInfo xxlJobInfo);

    XxlJobInfo loadById(@Param("id") int id);
}
