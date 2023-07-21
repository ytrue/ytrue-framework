package com.ytrue.ioc.context;

import com.ytrue.ioc.beans.BeansException;

/**
 * @author ytrue
 * @date 2022/10/10 15:51
 * @description ConfigurableApplicationContext
 */
public interface ConfigurableApplicationContext extends ApplicationContext {

    /**
     * 刷新容器
     *
     * @throws BeansException
     */
    void refresh() throws BeansException;

    /**
     * 注册虚拟机钩子
     */
    void registerShutdownHook();

    /**
     * 手动执行关闭
     */
    void close();
}
