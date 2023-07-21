package com.ytrue.ioc.context.support;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.factory.ConfigurableListableBeanFactory;
import com.ytrue.ioc.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author ytrue
 * @date 2022/10/10 15:56
 * @description AbstractRefreshableApplicationContext
 */
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

    private DefaultListableBeanFactory beanFactory;

    @Override
    protected void refreshBeanFactory() throws BeansException {
        DefaultListableBeanFactory beanFactory = createBeanFactory();
        loadBeanDefinitions(beanFactory);
        this.beanFactory = beanFactory;
    }

    /**
     * 创建DefaultListableBeanFactory
     *
     * @return
     */
    private DefaultListableBeanFactory createBeanFactory() {
        return new DefaultListableBeanFactory();
    }

    /**
     * 加载BeanDefinitions
     *
     * @param beanFactory
     */
    protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory);

    @Override
    protected ConfigurableListableBeanFactory getBeanFactory() {
        return beanFactory;
    }
}
