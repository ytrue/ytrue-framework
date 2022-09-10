package com.ytrue.orm.reflection.property;

import org.junit.Test;

/**
 * @author ytrue
 * @date 2022/8/19 14:05
 * @description PropertyNamerTest
 */
public class PropertyNamerTest {


    @Test
    public void testNamer() {

        System.out.println(PropertyNamer.methodToProperty("getName"));
        System.out.println(PropertyNamer.methodToProperty("setName"));
        System.out.println(PropertyNamer.methodToProperty("isTrue"));
    }

}
