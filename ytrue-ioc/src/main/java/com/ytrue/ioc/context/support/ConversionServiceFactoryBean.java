package com.ytrue.ioc.context.support;

import com.sun.istack.internal.Nullable;
import com.ytrue.ioc.beans.factory.FactoryBean;
import com.ytrue.ioc.beans.factory.InitializingBean;
import com.ytrue.ioc.core.convert.ConversionService;
import com.ytrue.ioc.core.convert.converter.Converter;
import com.ytrue.ioc.core.convert.converter.ConverterFactory;
import com.ytrue.ioc.core.convert.converter.ConverterRegistry;
import com.ytrue.ioc.core.convert.converter.GenericConverter;
import com.ytrue.ioc.core.convert.support.DefaultConversionService;
import com.ytrue.ioc.core.convert.support.GenericConversionService;

import java.util.Set;

/**
 * @author ytrue
 * @date 2022/10/20 16:35
 * @description 提供创建 ConversionService 工厂
 */
public class ConversionServiceFactoryBean implements FactoryBean<ConversionService>, InitializingBean {

    @Nullable
    private Set<?> converters;

    @Nullable
    private GenericConversionService conversionService;

    @Override
    public ConversionService getObject() throws Exception {
        return conversionService;
    }

    @Override
    public Class<?> getObjectType() {
        return conversionService.getClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.conversionService = new DefaultConversionService();
        registerConverters(converters, conversionService);
    }

    private void registerConverters(Set<?> converters, ConverterRegistry registry) {
        if (converters != null) {
            for (Object converter : converters) {
                if (converter instanceof GenericConverter) {
                    registry.addConverter((GenericConverter) converter);
                } else if (converter instanceof Converter<?, ?>) {
                    registry.addConverter((Converter<?, ?>) converter);
                } else if (converter instanceof ConverterFactory<?, ?>) {
                    registry.addConverterFactory((ConverterFactory<?, ?>) converter);
                } else {
                    throw new IllegalArgumentException("Each converter object must implement one of the " +
                            "Converter, ConverterFactory, or GenericConverter interfaces");
                }
            }
        }
    }

    public void setConverters(Set<?> converters) {
        this.converters = converters;
    }

}
