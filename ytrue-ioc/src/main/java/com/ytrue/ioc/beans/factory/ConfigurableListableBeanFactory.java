package com.ytrue.ioc.beans.factory;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.factory.config.AutowireCapableBeanFactory;
import com.ytrue.ioc.beans.factory.config.BeanDefinition;
import com.ytrue.ioc.beans.factory.config.BeanPostProcessor;
import com.ytrue.ioc.beans.factory.config.ConfigurableBeanFactory;

/**
 * @author ytrue
 * @date 2022/10/10 08:57
 * @description ConfigurableListableBeanFactory
 */
public interface ConfigurableListableBeanFactory extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

    /**
     * 获取BeanDefinition
     *
     * @param beanName
     * @return
     * @throws BeansException
     */
    BeanDefinition getBeanDefinition(String beanName) throws BeansException;


    /**
     * 提前实例化单例Bean对象
     *
     * @throws BeansException
     */
    void preInstantiateSingletons() throws BeansException;

    /**
     * 添加 BeanPostProcessor
     *
     * @param beanPostProcessor
     */
    @Override
    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);
}
