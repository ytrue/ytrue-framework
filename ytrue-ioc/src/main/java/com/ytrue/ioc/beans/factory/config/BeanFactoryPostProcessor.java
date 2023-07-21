package com.ytrue.ioc.beans.factory.config;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.factory.ConfigurableListableBeanFactory;

/**
 * @author ytrue
 * @date 2022/10/10 15:47
 * @description 允许自定义修改 BeanDefinition 属性信息
 */
public interface BeanFactoryPostProcessor {

    /**
     * 在所有的 BeanDefinition 加载完成后，实例化 Bean 对象之前，提供修改 BeanDefinition 属性的机制
     *
     * @param beanFactory
     * @throws BeansException
     */
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;
}
