package com.ytrue.orm.test;


import com.alibaba.fastjson.JSON;
import com.ytrue.orm.io.Resources;
import com.ytrue.orm.scripting.xmltags.OgnlClassResolver;
import com.ytrue.orm.session.SqlSession;
import com.ytrue.orm.session.SqlSessionFactory;
import com.ytrue.orm.session.SqlSessionFactoryBuilder;
import com.ytrue.orm.test.dao.IActivityDao;
import com.ytrue.orm.test.po.Activity;
import lombok.extern.slf4j.Slf4j;
import ognl.Ognl;
import ognl.OgnlException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

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
    public void test0Ognl() throws OgnlException {

        Activity activity = new Activity();
        activity.setActivityId(1l);
        Map<Object, OgnlClassResolver> context = Ognl.createDefaultContext(activity, new OgnlClassResolver());

        System.out.println(Ognl.getValue("activityId!=null", context, activity));
    }



    @Test
    public void test_queryActivityById1() throws IOException {
        // 1. 从SqlSessionFactory中获取SqlSession
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader("ytrue-orm-config.xml"));


        // 2. 请求对象
        Activity req = new Activity();
        req.setActivityId(100001L);

        // 3. 第一组：SqlSession
        // 3.1 开启 Session
        SqlSession sqlSession01 = sqlSessionFactory.openSession();
        // 3.2 获取映射器对象
        IActivityDao dao01 = sqlSession01.getMapper(IActivityDao.class);
        log.info("测试结果01：{}", JSON.toJSONString(dao01.queryActivityById(req)));
        sqlSession01.close();

        // 4. 第一组：SqlSession
        // 4.1 开启 Session
        SqlSession sqlSession02 = sqlSessionFactory.openSession();
        // 4.2 获取映射器对象
        IActivityDao dao02 = sqlSession02.getMapper(IActivityDao.class);
        log.info("测试结果02：{}", JSON.toJSONString(dao02.queryActivityById(req)));
        sqlSession02.close();
    }


    @Test
    public void test_queryActivityById() {

        // 2. 获取映射器对象
        IActivityDao dao = sqlSession.getMapper(IActivityDao.class);

        // 3. 测试验证
        Activity req = new Activity();
        req.setActivityId(100001L);

        log.info("测试结果：{}", JSON.toJSONString(dao.queryActivityById(req)));

//         sqlSession.commit();
        // sqlSession.clearCache();
//         sqlSession.close();

        log.info("测试结果：{}", JSON.toJSONString(dao.queryActivityById(req)));



    }


    @Test
    public void test_insert() {
        // 1. 获取映射器对象
        IActivityDao dao = sqlSession.getMapper(IActivityDao.class);

        Activity activity = new Activity();
        activity.setActivityId(10007L);
        activity.setActivityName("测试活动");
        activity.setActivityDesc("测试数据插入");
        activity.setCreator("ytrue");

        // 2. 测试验证
        Integer res = dao.insert(activity);
        sqlSession.commit();

        log.info("测试结果：count：{} idx：{}", res, JSON.toJSONString(activity.getId()));
    }


}
