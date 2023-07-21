package com.ytrue.ioc.core.convert.support;

import com.ytrue.ioc.core.convert.converter.ConverterRegistry;

/**
 * @author ytrue
 * @date 2022/10/20 16:33
 * @description A specialization of {@link GenericConversionService} configured by default
 * with converters appropriate for most environments.
 */
public class DefaultConversionService extends GenericConversionService {

    public DefaultConversionService() {
        addDefaultConverters(this);
    }

    public static void addDefaultConverters(ConverterRegistry converterRegistry) {
        // 添加各类类型转换工厂
        converterRegistry.addConverterFactory(new StringToNumberConverterFactory());
    }
}
