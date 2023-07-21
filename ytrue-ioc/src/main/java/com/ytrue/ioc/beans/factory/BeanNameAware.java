package com.ytrue.ioc.beans.factory;

/**
 * @author ytrue
 * @date 2022/10/12 10:31
 * @description BeanNameAware
 */
public interface BeanNameAware extends Aware {

    /**
     * 设置beanName
     *
     * @param name
     */
    void setBeanName(String name);

}
