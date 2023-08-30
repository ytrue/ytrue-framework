package com.ytrue.job.admin.dao;

import com.ytrue.job.admin.core.model.XxlJobUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-29 16:14
 * @description XxlJobUserDao
 */
@Mapper
public interface XxlJobUserDao {


    /**
     * 分页列表
     *
     * @param offset
     * @param pagesize
     * @param username
     * @param role
     * @return
     */
    List<XxlJobUser> pageList(@Param("offset") int offset,
                              @Param("pagesize") int pagesize,
                              @Param("username") String username,
                              @Param("role") int role);

    /**
     * 总数
     *
     * @param offset
     * @param pagesize
     * @param username
     * @param role
     * @return
     */
    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("username") String username,
                      @Param("role") int role);


    /**
     * 根据名称获取用户
     *
     * @param username
     * @return
     */
    XxlJobUser loadByUserName(@Param("username") String username);

    /**
     * 保存
     *
     * @param xxlJobUser
     * @return
     */
    int save(XxlJobUser xxlJobUser);

    /**
     * 更新
     *
     * @param xxlJobUser
     * @return
     */
    int update(XxlJobUser xxlJobUser);

    /**
     * 更新
     *
     * @param id
     * @return
     */
    int delete(@Param("id") int id);
}
