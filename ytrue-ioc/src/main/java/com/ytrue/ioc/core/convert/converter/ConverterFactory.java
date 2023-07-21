package com.ytrue.ioc.core.convert.converter;

/**
 * @author ytrue
 * @date 2022/10/20 08:42
 * @description 类型转换工厂
 */
public interface ConverterFactory<S, R> {

    /**
     * 获取将S转换为目标类型T的转换器，其中T也是R的实例。
     *
     * @param targetType
     * @param <T>
     * @return
     */
    <T extends R> Converter<S, T> getConverter(Class<T> targetType);
}
