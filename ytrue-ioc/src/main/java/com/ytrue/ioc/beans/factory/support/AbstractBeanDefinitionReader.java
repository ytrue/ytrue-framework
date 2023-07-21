package com.ytrue.ioc.beans.factory.support;

import com.ytrue.ioc.core.io.DefaultResourceLoader;
import com.ytrue.ioc.core.io.ResourceLoader;

/**
 * @author ytrue
 * @date 2022/10/9 14:30
 * @description AbstractBeanDefinitionReader
 */
public abstract class AbstractBeanDefinitionReader implements BeanDefinitionReader {


    private final BeanDefinitionRegistry registry;

    private ResourceLoader resourceLoader;

    public AbstractBeanDefinitionReader(BeanDefinitionRegistry registry) {
        this(registry, new DefaultResourceLoader());
    }

    public AbstractBeanDefinitionReader(BeanDefinitionRegistry registry, ResourceLoader resourceLoader) {
        this.registry = registry;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public BeanDefinitionRegistry getRegistry() {
        return registry;
    }

    @Override
    public ResourceLoader getResourceLoader() {
        return resourceLoader;
    }
}
