package com.ytrue.ioc.beans.factory.config;

import com.sun.istack.internal.Nullable;
import com.ytrue.ioc.beans.factory.HierarchicalBeanFactory;
import com.ytrue.ioc.core.convert.ConversionService;
import com.ytrue.ioc.util.StringValueResolver;

/**
 * @author ytrue
 * @date 2022/10/10 08:57
 * @description ConfigurableBeanFactory
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {

    String SCOPE_SINGLETON = "singleton";

    String SCOPE_PROTOTYPE = "prototype";

    /**
     * 添加BeanPostProcessor
     *
     * @param beanPostProcessor
     */
    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);


    /**
     * 销毁单例对象
     */
    void destroySingletons();

    /**
     * Add a String resolver for embedded values such as annotation attributes.
     *
     * @param valueResolver the String resolver to apply to embedded values
     * @since 3.0
     */
    void addEmbeddedValueResolver(StringValueResolver valueResolver);

    /**
     * Resolve the given embedded value, e.g. an annotation attribute.
     *
     * @param value the value to resolve
     * @return the resolved value (may be the original value as-is)
     * @since 3.0
     */
    String resolveEmbeddedValue(String value);


    /**
     * Specify a Spring 3.0 ConversionService to use for converting
     * property values, as an alternative to JavaBeans PropertyEditors.
     *
     * @param conversionService
     */
    void setConversionService(ConversionService conversionService);

    /**
     * Return the associated ConversionService, if any.
     *
     * @return
     */
    @Nullable
    ConversionService getConversionService();

}
