package com.ytrue.middleware.orm.spring.test;


import com.alibaba.fastjson.JSON;
import com.ytrue.middleware.orm.spring.test.dao.IActivityDao;
import com.ytrue.middleware.orm.spring.test.po.Activity;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ApiTest {


    @Test
    public void test_ClassPathXmlApplicationContext() {
        BeanFactory beanFactory = new ClassPathXmlApplicationContext("spring-config.xml");
        IActivityDao dao = beanFactory.getBean("IActivityDao", IActivityDao.class);
        Activity res = dao.queryActivityById(new Activity(100001L));


        System.out.println(JSON.toJSONString(res));
    }

}
