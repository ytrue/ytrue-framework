package com.ytrue.orm.test.dao;

import com.ytrue.orm.test.po.User;

import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/11 15:09
 * @description IUserDao
 */
public interface IUserDao {

    User queryUserInfoById(Long id);

    User queryUserInfo(User req);

    List<User> queryUserInfoList();

    int updateUserInfo(User req);

    void insertUserInfo(User req);

    int deleteUserInfoByUserId(String userId);

}
