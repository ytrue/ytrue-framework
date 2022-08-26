package com.ytrue.orm.builder;

import com.ytrue.orm.mapping.ParameterMapping;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.parsing.GenericTokenParser;
import com.ytrue.orm.parsing.TokenHandler;
import com.ytrue.orm.reflection.MetaClass;
import com.ytrue.orm.reflection.MetaObject;
import com.ytrue.orm.session.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ytrue
 * @date 2022/8/23 14:10
 * @description SQL 源码构建器
 */
public class SqlSourceBuilder extends BaseBuilder {

    private static final String parameterProperties = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";


    public SqlSourceBuilder(Configuration configuration) {
        super(configuration);
    }


    /**
     * 解析
     *
     * @param originalSql          源sql语句
     * @param parameterType        参数类型
     * @param additionalParameters 附加参数
     * @return
     */
    public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
        ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);

        // 把sql语句里面的 #{id} 里面的 id 替换成 ?
        GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
        String sql = parser.parse(originalSql);

        // 返回静态 SQL
        return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
    }


    private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {

        private List<ParameterMapping> parameterMappings = new ArrayList<>();
        private Class<?> parameterType;
        private MetaObject metaParameters;

        public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType, Map<String, Object> additionalParameters) {
            super(configuration);
            this.parameterType = parameterType;
            this.metaParameters = configuration.newMetaObject(additionalParameters);
        }

        public List<ParameterMapping> getParameterMappings() {
            return parameterMappings;
        }

        /**
         * @param content 就是 #{id} 里面的id ,#{name} 里面的name
         * @return
         */
        @Override
        public String handleToken(String content) {
            // 添加参数映射
            parameterMappings.add(buildParameterMapping(content));
            // 替换成?
            return "?";
        }

        /**
         * 构建参数映射
         *
         * @param content 就是 #{id} 里面的id ,#{name} 里面的name
         * @return
         */
        private ParameterMapping buildParameterMapping(String content) {
            // 先解析参数映射,就是转化成一个 HashMap | #{favouriteSection,jdbcType=VARCHAR}
            // key == property ,value == id
            Map<String, String> propertiesMap = new ParameterExpression(content);
            String property = propertiesMap.get("property");

            Class<?> propertyType;

            /*
                三种情况
                    1. parameterType == Long 类型 直接赋值因为在类型处理注册器有
                    2. 类型处理注册器没有，但是它不为空,这个时候就要把它当成一个对象处理
                        ，如果这个对象里面没有 property对应的 get方法就是返回Object 负责返回对应的类型

                    3. 直接返回Object类型
             */
            if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
                propertyType = parameterType;
            } else if (property != null) {
                // 反射处理对象
                MetaClass metaClass = MetaClass.forClass(parameterType);
                // 如果是一个对象，这里回去判断 getId() 这个方法是否存在,存在就返回get方法返回的类型
                if (metaClass.hasGetter(property)) {
                    propertyType = metaClass.getGetterType(property);
                } else {
                    propertyType = Object.class;
                }
            } else {
                propertyType = Object.class;
            }
            // 如果parameterType ==null 这里 的类型处理器是空会导致null
            return new ParameterMapping.Builder(configuration, property, propertyType).build();

        }

    }
}
