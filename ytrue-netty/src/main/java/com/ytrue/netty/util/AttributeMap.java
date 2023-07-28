package com.ytrue.netty.util;

/**
 * @author ytrue
 * @date 2023-07-28 13:46
 * @description netty自定义的map接口，该接口的实现类是一个map，
 */
public interface AttributeMap {

    /**
     * 该方法的作用是添加一个数据，并且返回一个Attribute类型的对象,对象中封装着map的key和value
     * @param key
     * @return
     * @param <T>
     */
    <T> Attribute<T> attr(AttributeKey<T> key);

    /**
     * 判断key是否存在
     *
     * @param key
     * @param <T>
     * @return
     */
    <T> boolean hasAttr(AttributeKey<T> key);
}
