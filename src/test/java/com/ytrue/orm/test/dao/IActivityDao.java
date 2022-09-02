package com.ytrue.orm.test.dao;


import com.ytrue.orm.test.po.Activity;

public interface IActivityDao {

    Activity queryActivityById(Activity activity);

    Integer insert(Activity activity);
}
