package com.ytrue.ioc.core.convert.converter;

import cn.hutool.core.lang.Assert;

import java.util.Set;

/**
 * @author ytrue
 * @date 2022/10/20 08:45
 * @description 通用的转换接口
 */
public interface GenericConverter {

    /**
     *  Return the source and target types that this converter can convert between.
     * @return
     */
    Set<ConvertiblePair> getConvertibleTypes();

    /**
     * Convert the source object to the targetType described by the {@code TypeDescriptor}.
     *
     * @param source     the source object to convert (may be {@code null})
     * @param sourceType the type descriptor of the field we are converting from
     * @param targetType the type descriptor of the field we are converting to
     * @param source
     * @param sourceType
     * @param targetType
     * @return the converted object
     * @return
     */
    Object convert(Object source, Class sourceType, Class targetType);

    /**
     * Holder for a source-to-target class pair.
     */
    final class ConvertiblePair {
        private final Class<?> sourceType;

        private final Class<?> targetType;

        public ConvertiblePair(Class<?> sourceType, Class<?> targetType) {
            Assert.notNull(sourceType, "Source type must not be null");
            Assert.notNull(targetType, "Target type must not be null");
            this.sourceType = sourceType;
            this.targetType = targetType;
        }

        public Class<?> getSourceType() {
            return this.sourceType;
        }

        public Class<?> getTargetType() {
            return this.targetType;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != ConvertiblePair.class) {
                return false;
            }
            ConvertiblePair other = (ConvertiblePair) obj;
            return this.sourceType.equals(other.sourceType) && this.targetType.equals(other.targetType);

        }

        @Override
        public int hashCode() {
            return this.sourceType.hashCode() * 31 + this.targetType.hashCode();
        }
    }
}
