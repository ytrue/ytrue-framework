package com.ytrue.ioc.core.convert.converter;

/**
 * @author ytrue
 * @date 2022/10/20 08:41
 * @description 类型转换处理接口
 */
public interface Converter<S, T> {

    /**
     * 转换
     *
     * @param source
     * @return
     */
    T convert(S source);
}
