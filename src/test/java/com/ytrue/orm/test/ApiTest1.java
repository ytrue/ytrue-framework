package com.ytrue.orm.test;


import com.alibaba.fastjson.JSON;
import com.ytrue.orm.io.Resources;
import com.ytrue.orm.session.SqlSession;
import com.ytrue.orm.session.SqlSessionFactory;
import com.ytrue.orm.session.SqlSessionFactoryBuilder;
import com.ytrue.orm.test.dao.IActivityDao;
import com.ytrue.orm.test.po.Activity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

@Slf4j
public class ApiTest1 {


    private SqlSession sqlSession;

    @Before
    public void init() throws IOException {
        // 1. 从SqlSessionFactory中获取SqlSession
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader("ytrue-orm-config.xml"));
        sqlSession = sqlSessionFactory.openSession();
    }

    @Test
    public void test_queryActivityById() {

        // 2. 获取映射器对象
        IActivityDao dao = sqlSession.getMapper(IActivityDao.class);
        // 3. 测试验证
        Activity req = new Activity();
        req.setActivityId(100001L);
        Activity res = dao.queryActivityById(req);
        log.info("测试结果：{}", JSON.toJSONString(res));

//        // 1. 获取映射器对象
//        IActivityDao dao = sqlSession.getMapper(IActivityDao.class);
//        // 2. 测试验证
//        Activity res = dao.queryActivityById(100001L);
//        log.info("测试结果：{}", JSON.toJSONString(res));
    }


    @Test
    public void test_insert() {
        // 1. 获取映射器对象
        IActivityDao dao = sqlSession.getMapper(IActivityDao.class);

        Activity activity = new Activity();
        activity.setActivityId(10004L);
        activity.setActivityName("测试活动");
        activity.setActivityDesc("测试数据插入");
        activity.setCreator("xiaofuge");

        // 2. 测试验证
        Integer res = dao.insert(activity);
        sqlSession.commit();

        log.info("测试结果：count：{} idx：{}", res, JSON.toJSONString(activity.getId()));
    }


}
