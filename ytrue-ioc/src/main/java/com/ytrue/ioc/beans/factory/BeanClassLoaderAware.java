package com.ytrue.ioc.beans.factory;

/**
 * @author ytrue
 * @date 2022/10/12 10:31
 * @description BeanClassLoaderAware
 */
public interface BeanClassLoaderAware extends Aware {

    /**
     * 设置ClassLoader
     * @param classLoader
     */
    void setBeanClassLoader(ClassLoader classLoader);
}
