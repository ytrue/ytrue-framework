package com.ytrue.orm.test.dao;

/**
 * @author ytrue
 * @date 2022/7/11 11:09
 * @description IUserDao
 */
public interface IUserDao {

    String queryUserName(String uId);

    Integer queryUserAge(String uId);
}
