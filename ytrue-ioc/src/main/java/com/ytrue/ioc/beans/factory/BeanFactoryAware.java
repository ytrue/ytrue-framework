package com.ytrue.ioc.beans.factory;

import com.ytrue.ioc.beans.BeansException;

/**
 * @author ytrue
 * @date 2022/10/12 10:32
 * @description BeanFactoryAware
 */
public interface BeanFactoryAware extends Aware {

    /**
     * 设置BeanFactory
     * @param beanFactory
     * @throws BeansException
     */
    void setBeanFactory(BeanFactory beanFactory) throws BeansException;

}
