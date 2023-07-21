package com.ytrue.ioc.beans.factory;

import com.ytrue.ioc.beans.BeansException;

/**
 * @author ytrue
 * @date 2022/9/28 09:15
 * @description BeanFactory
 */
public interface BeanFactory {

    /**
     * 根据name获取bean
     *
     * @param name
     * @return
     * @throws BeansException
     */
    Object getBean(String name) throws BeansException;

    /**
     * 获取bean
     *
     * @param name
     * @param args
     * @return
     * @throws BeansException
     */
    Object getBean(String name, Object... args) throws BeansException;


    /**
     * 获取bean
     *
     * @param name
     * @param requiredType
     * @param <T>
     * @return
     * @throws BeansException
     */
    <T> T getBean(String name, Class<T> requiredType) throws BeansException;

    /**
     * 获取bean
     *
     * @param requiredType
     * @param <T>
     * @return
     * @throws BeansException
     */
    <T> T getBean(Class<T> requiredType) throws BeansException;


    /**
     * 是否包含
     * @param name
     * @return
     */
    boolean containsBean(String name);


}
