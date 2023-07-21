package com.ytrue.ioc.beans.factory.support;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.core.io.Resource;
import com.ytrue.ioc.core.io.ResourceLoader;

/**
 * @author ytrue
 * @date 2022/10/9 14:30
 * @description BeanDefinitionReader BeanDefinition读取
 */
public interface BeanDefinitionReader {

    /**
     * 获取 BeanDefinitionRegistry
     *
     * @return
     */
    BeanDefinitionRegistry getRegistry();

    /**
     * 获取 ResourceLoader
     *
     * @return
     */
    ResourceLoader getResourceLoader();

    /**
     * 加载BeanDefinition
     *
     * @param resource
     * @throws BeansException
     */
    void loadBeanDefinitions(Resource resource) throws BeansException;

    /**
     * 加载BeanDefinition
     *
     * @param resources
     * @throws BeansException
     */
    void loadBeanDefinitions(Resource... resources) throws BeansException;

    /**
     * 加载BeanDefinition
     *
     * @param location
     * @throws BeansException
     */
    void loadBeanDefinitions(String location) throws BeansException;

    /**
     * 加载BeanDefinition
     *
     * @param locations
     * @throws BeansException
     */
    void loadBeanDefinitions(String... locations) throws BeansException;
}
