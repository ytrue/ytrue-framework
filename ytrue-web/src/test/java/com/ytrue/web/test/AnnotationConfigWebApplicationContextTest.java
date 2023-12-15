package com.ytrue.web.test;

import com.ytrue.web.context.AnnotationConfigWebApplicationContext;
import com.ytrue.web.test.beans.AppConfig;
import com.ytrue.web.test.beans.User;
import org.junit.Test;

/**
 * @author ytrue
 * @date 2023-12-15 9:37
 * @description AnnotationConfigWebApplicationContextTest
 */
public class AnnotationConfigWebApplicationContextTest {


    @Test
    public void test01() {


        AnnotationConfigWebApplicationContext ac = new AnnotationConfigWebApplicationContext();
        ac.register(AppConfig.class);
        ac.refresh();

        User bean = ac.getBean(User.class);
        System.out.println(bean);


    }
}
