package com.ytrue.job.admin.dao;

import com.ytrue.job.admin.core.model.XxlJobGroup;
import org.apache.ibatis.annotations.Param;

/**
 * @author ytrue
 * @date 2023-08-29 16:13
 * @description XxlJobGroupDao
 */
public interface XxlJobGroupDao {

    XxlJobGroup load(@Param("id") int id);
}
