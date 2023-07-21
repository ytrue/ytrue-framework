package com.ytrue.ioc.core.convert.support;

import com.ytrue.ioc.core.convert.ConversionService;
import com.ytrue.ioc.core.convert.converter.Converter;
import com.ytrue.ioc.core.convert.converter.ConverterFactory;
import com.ytrue.ioc.core.convert.converter.ConverterRegistry;
import com.ytrue.ioc.core.convert.converter.GenericConverter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author ytrue
 * @date 2022/10/20 15:56
 * @description Base ConversionService implementation suitable for use in most environments.
 * Indirectly implements ConverterRegistry as registration API through the
 * ConfigurableConversionService interface.
 */
public class GenericConversionService implements ConversionService, ConverterRegistry {

    private Map<GenericConverter.ConvertiblePair, GenericConverter> converters = new HashMap<>();

    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        GenericConverter converter = getConverter(sourceType, targetType);
        return converter != null;
    }

    @Override
    public <T> T convert(Object source, Class<T> targetType) {
        Class<?> sourceType = source.getClass();
        GenericConverter converter = getConverter(sourceType, targetType);
        return (T) converter.convert(source, sourceType, targetType);
    }

    @Override
    public void addConverter(Converter<?, ?> converter) {
        GenericConverter.ConvertiblePair typeInfo = getRequiredTypeInfo(converter);
        ConverterAdapter converterAdapter = new ConverterAdapter(typeInfo, converter);
        for (GenericConverter.ConvertiblePair convertibleType : converterAdapter.getConvertibleTypes()) {
            converters.put(convertibleType, converterAdapter);
        }
    }

    @Override
    public void addConverter(GenericConverter converter) {
        for (GenericConverter.ConvertiblePair convertibleType : converter.getConvertibleTypes()) {
            converters.put(convertibleType, converter);
        }
    }

    @Override
    public void addConverterFactory(ConverterFactory<?, ?> converterFactory) {
        GenericConverter.ConvertiblePair typeInfo = getRequiredTypeInfo(converterFactory);
        ConverterFactoryAdapter converterFactoryAdapter = new ConverterFactoryAdapter(typeInfo, converterFactory);
        for (GenericConverter.ConvertiblePair convertibleType : converterFactoryAdapter.getConvertibleTypes()) {
            converters.put(convertibleType, converterFactoryAdapter);
        }
    }

    /**
     * 就是获取实现Converter<S, T> 的 s 和 t 再包装成GenericConverter.ConvertiblePair
     *
     * @param object
     * @return
     */
    private GenericConverter.ConvertiblePair getRequiredTypeInfo(Object object) {
        // 回表示当前对象所表示的类或接口直接实现的接口类型
        Type[] types = object.getClass().getGenericInterfaces();
        ParameterizedType parameterized = (ParameterizedType) types[0];
        Type[] actualTypeArguments = parameterized.getActualTypeArguments();

        Class sourceType = (Class) actualTypeArguments[0];
        Class targetType = (Class) actualTypeArguments[1];

        return new GenericConverter.ConvertiblePair(sourceType, targetType);
    }


    /**
     * 在converters里面获取对应的转换器
     *
     * @param sourceType
     * @param targetType
     * @return
     */
    protected GenericConverter getConverter(Class<?> sourceType, Class<?> targetType) {
        List<Class<?>> sourceCandidates = getClassHierarchy(sourceType);
        List<Class<?>> targetCandidates = getClassHierarchy(targetType);
        // 循环处理
        for (Class<?> sourceCandidate : sourceCandidates) {
            // 再循环
            for (Class<?> targetCandidate : targetCandidates) {
                GenericConverter.ConvertiblePair convertiblePair = new GenericConverter.ConvertiblePair(sourceCandidate, targetCandidate);
                // 获取对应的转换器
                GenericConverter converter = this.converters.get(convertiblePair);
                if (converter != null) {
                    return converter;
                }
            }
        }
        return null;
    }


    /**
     * 获取当前类以及所有父类
     *
     * @param clazz
     * @return
     */
    private List<Class<?>> getClassHierarchy(Class<?> clazz) {
        List<Class<?>> hierarchy = new ArrayList<>();
        while (clazz != null) {
            hierarchy.add(clazz);
            clazz = clazz.getSuperclass();
        }
        return hierarchy;
    }

    private final class ConverterAdapter implements GenericConverter {

        private final ConvertiblePair typeInfo;

        private final Converter<Object, Object> converter;


        public ConverterAdapter(ConvertiblePair typeInfo, Converter<?, ?> converter) {
            this.typeInfo = typeInfo;
            this.converter = (Converter<Object, Object>) converter;
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(typeInfo);
        }

        @Override
        public Object convert(Object source, Class sourceType, Class targetType) {
            return converter.convert(source);
        }
    }


    private final class ConverterFactoryAdapter implements GenericConverter {

        private final ConvertiblePair typeInfo;

        private final ConverterFactory<Object, Object> converterFactory;

        public ConverterFactoryAdapter(ConvertiblePair typeInfo, ConverterFactory<?, ?> converterFactory) {
            this.typeInfo = typeInfo;
            this.converterFactory = (ConverterFactory<Object, Object>) converterFactory;
        }


        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            // 返回一个Set集合，其中的元素为一个只包含object(obj)的不可变集合
            return Collections.singleton(typeInfo);
        }

        @Override
        public Object convert(Object source, Class sourceType, Class targetType) {
            return converterFactory.getConverter(targetType).convert(source);
        }
    }
}
