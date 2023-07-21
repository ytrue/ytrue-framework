package com.ytrue.ioc.core.convert;

import com.sun.istack.internal.Nullable;

/**
 * @author ytrue
 * @date 2022/10/20 15:49
 * @description 类型转换抽象接口
 */
public interface ConversionService {

    /**
     * 是否能转换
     * Return {@code true} if objects of {@code sourceType} can be converted to the {@code targetType}.
     *
     * @param sourceType
     * @param targetType
     * @return
     */
    boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType);

    /**
     * 转换
     * Convert the given {@code source} to the specified {@code targetType}.
     *
     * @param source
     * @param targetType
     * @param <T>
     * @return
     */
    <T> T convert(Object source, Class<T> targetType);

}
