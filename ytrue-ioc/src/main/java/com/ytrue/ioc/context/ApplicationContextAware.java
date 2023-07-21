package com.ytrue.ioc.context;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.factory.Aware;

/**
 * @author ytrue
 * @date 2022/10/12 10:32
 * @description ApplicationContextAware
 */
public interface ApplicationContextAware extends Aware {

    /**
     * 设置ApplicationContext
     *
     * @param applicationContext
     * @throws BeansException
     */
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException;

}
