package com.ytrue.ioc.core.convert.support;

import com.sun.istack.internal.Nullable;
import com.ytrue.ioc.core.convert.converter.Converter;
import com.ytrue.ioc.core.convert.converter.ConverterFactory;
import com.ytrue.ioc.util.NumberUtils;

/**
 * @author ytrue
 * @date 2022/10/20 15:51
 * @description StringToNumberConverterFactory
 */
public class StringToNumberConverterFactory implements ConverterFactory<String, Number> {

    @Override
    public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToNumber<>(targetType);
    }

    private static final class StringToNumber<T extends Number> implements Converter<String, T> {

        private final Class<T> targetType;

        public StringToNumber(Class<T> targetType) {
            this.targetType = targetType;
        }


        @Override
        @Nullable
        public T convert(String source) {
            if (source.isEmpty()) {
                return null;
            }
            // 转换成数字类型
            return NumberUtils.parseNumber(source, this.targetType);
        }
    }
}
