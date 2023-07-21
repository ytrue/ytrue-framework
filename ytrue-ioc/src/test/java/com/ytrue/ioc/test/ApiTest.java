package com.ytrue.ioc.test;

import com.ytrue.ioc.context.support.ClassPathXmlApplicationContext;
import com.ytrue.ioc.test.bean.Husband;
import org.junit.Test;

/**
 * @author ytrue
 * @date 2022/10/18 10:46
 * @description ApiTest
 */
public class ApiTest {

    @Test
    public void test_scan() {


        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring.xml");
        Husband husband = applicationContext.getBean("husband", Husband.class);
        System.out.println("测试结果：" + husband);
    }

}
