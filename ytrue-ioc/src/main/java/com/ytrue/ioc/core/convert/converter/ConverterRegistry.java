package com.ytrue.ioc.core.convert.converter;

/**
 * @author ytrue
 * @date 2022/10/20 08:43
 * @description 类型转换注册接口
 */
public interface ConverterRegistry {

    /**
     * Add a plain converter to this registry.
     * The convertible source/target type pair is derived from the Converter's parameterized types.
     *
     * @param converter
     * @throws IllegalArgumentException if the parameterized types could not be resolved
     */
    void addConverter(Converter<?, ?> converter);

    /**
     * Add a generic converter to this registry.
     *
     * @param converter
     */
    void addConverter(GenericConverter converter);

    /**
     * Add a ranged converter factory to this registry.
     * The convertible source/target type pair is derived from the ConverterFactory's parameterized types.
     *
     * @param converterFactory
     * @throws IllegalArgumentException if the parameterized types could not be resolved
     */
    void addConverterFactory(ConverterFactory<?, ?> converterFactory);
}
