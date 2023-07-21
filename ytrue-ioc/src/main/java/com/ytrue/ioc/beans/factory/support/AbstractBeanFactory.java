package com.ytrue.ioc.beans.factory.support;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.factory.FactoryBean;
import com.ytrue.ioc.beans.factory.config.BeanDefinition;
import com.ytrue.ioc.beans.factory.config.BeanPostProcessor;
import com.ytrue.ioc.beans.factory.config.ConfigurableBeanFactory;
import com.ytrue.ioc.core.convert.ConversionService;
import com.ytrue.ioc.util.ClassUtils;
import com.ytrue.ioc.util.StringValueResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/9/28 09:18
 * @description AbstractBeanFactory
 */
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {


    /**
     * ClassLoader to resolve bean class names with, if necessary
     */
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    /**
     * BeanPostProcessors to apply in createBean
     */
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    /**
     * String resolvers to apply e.g. to annotation attribute values
     */
    private final List<StringValueResolver> embeddedValueResolvers = new ArrayList<>();

    private ConversionService conversionService;

    @Override
    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public ConversionService getConversionService() {
        return conversionService;
    }

    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return doGetBean(name, args);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return (T) getBean(name);
    }


    protected <T> T doGetBean(final String name, final Object[] args) {
        Object sharedInstance = getSingleton(name);
        if (sharedInstance != null) {
            // 如果是 FactoryBean，则需要调用 FactoryBean#getObject
            return (T) getObjectForBeanInstance(sharedInstance, name);
        }

        BeanDefinition beanDefinition = getBeanDefinition(name);
        Object bean = createBean(name, beanDefinition, args);
        return (T) getObjectForBeanInstance(bean, name);
    }

    /**
     * 获取对象
     *
     * @param beanInstance
     * @param beanName
     * @return
     */
    private Object getObjectForBeanInstance(Object beanInstance, String beanName) {
        // 如果没有实现FactoryBean就直接返回
        if (!(beanInstance instanceof FactoryBean)) {
            return beanInstance;
        }

        // 获取factoryBeanObjectCache对应的对象
        Object object = getCachedObjectForFactoryBean(beanName);

        if (object == null) {
            FactoryBean<?> factoryBean = (FactoryBean<?>) beanInstance;
            object = getObjectFromFactoryBean(factoryBean, beanName);
        }

        return object;
    }


    /**
     * 获取BeanDefinition
     *
     * @param beanName
     * @return
     * @throws BeansException
     */
    protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

    /**
     * 创建bean
     *
     * @param beanName
     * @param beanDefinition
     * @param args
     * @return
     * @throws BeansException
     */
    protected abstract Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException;


    @Override
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.remove(beanPostProcessor);
        this.beanPostProcessors.add(beanPostProcessor);
    }

    /**
     * Return the list of BeanPostProcessors that will get applied
     * to beans created with this factory.
     */
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanPostProcessors;
    }


    public ClassLoader getBeanClassLoader() {
        return this.beanClassLoader;
    }


    @Override
    public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
        this.embeddedValueResolvers.add(valueResolver);
    }

    @Override
    public String resolveEmbeddedValue(String value) {
        String result = value;
        for (StringValueResolver resolver : this.embeddedValueResolvers) {
            result = resolver.resolveStringValue(result);
        }
        return result;
    }

    @Override
    public boolean containsBean(String name) {
        return containsBeanDefinition(name);
    }

    /**
     * 是否存在
     *
     * @param beanName
     * @return
     */
    protected abstract boolean containsBeanDefinition(String beanName);
}
