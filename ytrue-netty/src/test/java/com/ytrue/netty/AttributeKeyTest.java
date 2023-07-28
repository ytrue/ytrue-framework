package com.ytrue.netty;

import com.ytrue.netty.util.AttributeKey;

/**
 * @author ytrue
 * @date 2023-07-28 14:20
 * @description AttributeKeyTest
 */
public class AttributeKeyTest {

    public static void main(String[] args) {


        AttributeKey<Object> objectAttributeKey = AttributeKey.valueOf("222");
        AttributeKey<Object> objectAttributeKey1 = AttributeKey.valueOf("333");


        System.out.println(objectAttributeKey);
    }
}
