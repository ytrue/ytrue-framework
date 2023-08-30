package com.ytrue.job.admin.dao;

import com.ytrue.job.admin.core.model.XxlJobGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-29 16:13
 * @description XxlJobGroupDao
 */
@Mapper
public interface XxlJobGroupDao {

    XxlJobGroup load(@Param("id") int id);

    List<XxlJobGroup> findAll();


    List<XxlJobGroup> pageList(@Param("offset") int offset,
                               @Param("pagesize") int pagesize,
                               @Param("appname") String appname,
                               @Param("title") String title);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("appname") String appname,
                      @Param("title") String title);


    int save(XxlJobGroup xxlJobGroup);

    int update(XxlJobGroup xxlJobGroup);

    int remove(@Param("id") int id);

    List<XxlJobGroup> findByAddressType(@Param("addressType") int addressType);
}
