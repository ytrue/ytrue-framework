package com.ytrue.job.admin.dao;

import com.ytrue.job.admin.core.model.XxlJobRegistry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-29 9:59
 * @description XxlJobRegistryDao
 */
@Mapper
public interface XxlJobRegistryDao {

    /**
     * 查询对应的
     *
     * @param timeout
     * @param nowTime
     * @return
     */
    List<Integer> findDead(@Param("timeout") int timeout,
                           @Param("nowTime") Date nowTime);

    /**
     * 删除对应的
     *
     * @param ids
     * @return
     */
    int removeDead(@Param("ids") List<Integer> ids);

    /**
     * 查询所有
     *
     * @param timeout
     * @param nowTime
     * @return
     */
    List<XxlJobRegistry> findAll(@Param("timeout") int timeout,
                                 @Param("nowTime") Date nowTime);

    /**
     * 更新
     *
     * @param registryGroup
     * @param registryKey
     * @param registryValue
     * @param updateTime
     * @return
     */
    int registryUpdate(@Param("registryGroup") String registryGroup,
                       @Param("registryKey") String registryKey,
                       @Param("registryValue") String registryValue,
                       @Param("updateTime") Date updateTime);

    /**
     * 新增
     *
     * @param registryGroup
     * @param registryKey
     * @param registryValue
     * @param updateTime
     * @return
     */
    int registrySave(@Param("registryGroup") String registryGroup,
                     @Param("registryKey") String registryKey,
                     @Param("registryValue") String registryValue,
                     @Param("updateTime") Date updateTime);


    /**
     * 删除
     *
     * @param registryGroup
     * @param registryKey
     * @param registryValue
     * @return
     */
    int registryDelete(@Param("registryGroup") String registryGroup,
                       @Param("registryKey") String registryKey,
                       @Param("registryValue") String registryValue);
}
