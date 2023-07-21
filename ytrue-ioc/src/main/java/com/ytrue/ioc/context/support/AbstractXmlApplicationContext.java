package com.ytrue.ioc.context.support;

import com.ytrue.ioc.beans.factory.support.DefaultListableBeanFactory;
import com.ytrue.ioc.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * @author ytrue
 * @date 2022/10/10 15:56
 * @description AbstractXmlApplicationContext
 */
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableApplicationContext {

    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory, this);
        String[] configLocations = getConfigLocations();
        if (null != configLocations) {
            beanDefinitionReader.loadBeanDefinitions(configLocations);
        }
    }

    /**
     * 获取配置位置
     *
     * @return
     */
    protected abstract String[] getConfigLocations();
}
