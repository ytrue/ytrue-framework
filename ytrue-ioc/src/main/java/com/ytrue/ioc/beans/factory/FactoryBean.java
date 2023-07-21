package com.ytrue.ioc.beans.factory;

/**
 * @author ytrue
 * @date 2022/10/12 11:07
 * @description 是用于生产Bean对象的类
 */
public interface FactoryBean<T> {

    /**
     * 获取对象
     *
     * @return
     * @throws Exception
     */
    T getObject() throws Exception;

    /**
     * bean类型
     *
     * @return
     */
    Class<?> getObjectType();

    /**
     * 是否单例
     *
     * @return
     */
    boolean isSingleton();

}
