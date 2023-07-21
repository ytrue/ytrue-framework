package com.ytrue.ioc.beans.factory.config;

/**
 * @author ytrue
 * @date 2022/9/28 09:16
 * @description 单例注册表
 */
public interface SingletonBeanRegistry {
    /**
     * 获取单例
     *
     * @param beanName
     * @return
     */
    Object getSingleton(String beanName);

    /**
     * 注册
     * @param beanName
     * @param singletonObject
     */
    void registerSingleton(String beanName, Object singletonObject);
}
