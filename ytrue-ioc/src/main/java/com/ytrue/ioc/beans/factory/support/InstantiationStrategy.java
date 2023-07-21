package com.ytrue.ioc.beans.factory.support;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.factory.config.BeanDefinition;

import java.lang.reflect.Constructor;

/**
 * @author ytrue
 * @date 2022/9/28 16:35
 * @description Bean 实例化策略
 */
public interface InstantiationStrategy {

    /**
     * 实例化
     * @param beanDefinition
     * @param beanName
     * @param ctor
     * @param args
     * @return
     * @throws BeansException
     */
    Object instantiate(BeanDefinition beanDefinition, String beanName, Constructor ctor, Object[] args) throws BeansException;
}
