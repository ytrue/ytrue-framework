package com.ytrue.orm.reflection.property;

import org.junit.Test;

/**
 * @author ytrue
 * @date 2022/8/19 14:15
 * @description PropertyTokenizerTest
 */
public class PropertyTokenizerTest {


    @Test
    public void test01() {


        PropertyTokenizer propertyTokenizer = new PropertyTokenizer("list[0]");





        System.out.println(propertyTokenizer);
        System.out.println(new PropertyTokenizer("list"));

    }
}
