package com.ytrue.orm.mapping;

import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.type.JdbcType;
import com.ytrue.orm.type.TypeHandler;
import com.ytrue.orm.type.TypeHandlerRegistry;
import lombok.Getter;

/**
 * @author ytrue
 * @date 2022/8/23 14:21
 * @description 参数映射 #{property,javaType=int,jdbcType=NUMERIC}
 */
@Getter
public class ParameterMapping {

    private Configuration configuration;

    /**
     * #{name} 里面的 name
     */
    private String property;

    /**
     * javaType = int  对应的java类型
     */
    private Class<?> javaType;

    /**
     * jdbcType=NUMERIC  数据类型
     */
    private JdbcType jdbcType;

    /**
     * 参数处理器类型
     */
    private TypeHandler<?> typeHandler;

    /**
     * 构建者设计模式
     */
    public static class Builder {

        private ParameterMapping parameterMapping = new ParameterMapping();

        public Builder(Configuration configuration, String property, Class<?> javaType) {
            parameterMapping.configuration = configuration;
            parameterMapping.property = property;
            parameterMapping.javaType = javaType;
        }

        public Builder javaType(Class<?> javaType) {
            parameterMapping.javaType = javaType;
            return this;
        }

        public Builder jdbcType(JdbcType jdbcType) {
            parameterMapping.jdbcType = jdbcType;
            return this;
        }

        public ParameterMapping build() {
            if (parameterMapping.typeHandler == null && parameterMapping.javaType != null) {

                Configuration configuration = parameterMapping.configuration;
                TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
                // 获取对应的类型处理器
                parameterMapping.typeHandler = typeHandlerRegistry.getTypeHandler(parameterMapping.javaType, parameterMapping.jdbcType);
            }

            return parameterMapping;
        }

    }

}
