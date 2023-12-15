package com.ytrue.web.test;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

/**
 * @author ytrue
 * @date 2023-12-15 11:33
 * @description ConvertTest
 */
public class ConvertTest {

    @Test
    public void test01() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        Long l = Long.class.getConstructor(String.class).newInstance("123");

        System.out.println(l);
    }
}
