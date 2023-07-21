package com.ytrue.ioc.beans.factory;

import com.ytrue.ioc.beans.BeansException;

import java.util.Map;

/**
 * @author ytrue
 * @date 2022/10/10 08:55
 * @description ListableBeanFactory
 */
public interface ListableBeanFactory extends BeanFactory {

    /**
     * 按照类型返回 Bean 实例
     *
     * @param type
     * @param <T>
     * @return
     * @throws BeansException
     */
    <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException;

    /**
     * 返回注册表中所有的Bean名称
     * @return String[]
     */
    String[] getBeanDefinitionNames();
}
