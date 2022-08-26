package com.ytrue.orm.test.dao;

import com.ytrue.orm.test.po.User;

/**
 * @author ytrue
 * @date 2022/8/11 15:09
 * @description IUserDao
 */
public interface IUserDao {

    User queryUserInfoById(Long uId, String name);

}
