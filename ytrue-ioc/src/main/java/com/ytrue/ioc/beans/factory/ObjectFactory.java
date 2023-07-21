package com.ytrue.ioc.beans.factory;

import com.ytrue.ioc.beans.BeansException;

/**
 * @author ytrue
 * @date 2022/10/19 14:02
 * @description ObjectFactory
 */
public interface ObjectFactory<T> {

    /**
     * 获取对象
     * @return
     * @throws BeansException
     */
    T getObject() throws BeansException;
}
