package com.ytrue.orm.builder;

import com.ytrue.orm.mapping.ParameterMapping;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.parsing.GenericTokenParser;
import com.ytrue.orm.parsing.TokenHandler;
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
            Map<String, String> propertiesMap = new ParameterExpression(content);
            String property = propertiesMap.get("property");
            Class<?> propertyType = parameterType;

            return ParameterMapping
                    .builder()
                    .configuration(configuration)
                    .property(property)
                    .javaType(propertyType)
                    .build();
        }

    }
}
