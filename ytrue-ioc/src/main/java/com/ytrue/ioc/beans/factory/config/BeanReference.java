package com.ytrue.ioc.beans.factory.config;

/**
 * @author ytrue
 * @date 2022/9/29 09:57
 * @description Bean 的引用
 */
public class BeanReference {

    private final String beanName;

    public BeanReference(String beanName) {
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }
}
