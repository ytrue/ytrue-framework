package com.ytrue.orm.mapping;

import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.type.JdbcType;
import com.ytrue.orm.type.TypeHandler;
import com.ytrue.orm.type.TypeHandlerRegistry;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/25 14:59
 * @description 结果映射
 */
@Data
public class ResultMapping {

    private Configuration configuration;

    /**
     * 属性
     */
    private String property;

    /**
     * 字段
     */
    private String column;

    /**
     * java类型
     */
    private Class<?> javaType;

    /**
     * jdbc类型
     */
    private JdbcType jdbcType;

    /**
     * 类型处理器
     */
    private TypeHandler<?> typeHandler;

    /**
     * 结构标记
     */
    private List<ResultFlag> flags;

    public static class Builder {

        private ResultMapping resultMapping = new ResultMapping();

        public Builder(Configuration configuration, String property, String column, Class<?> javaType) {
            resultMapping.configuration = configuration;
            resultMapping.property = property;
            resultMapping.column = column;
            resultMapping.javaType = javaType;
            resultMapping.flags = new ArrayList<>();
        }

        public Builder typeHandler(TypeHandler<?> typeHandler) {
            resultMapping.typeHandler = typeHandler;
            return this;
        }

        public Builder flags(List<ResultFlag> flags) {
            resultMapping.flags = flags;
            return this;
        }

        public ResultMapping build() {
            resolveTypeHandler();
            return resultMapping;
        }

        private void resolveTypeHandler() {
            if (resultMapping.typeHandler == null && resultMapping.javaType != null) {
                Configuration configuration = resultMapping.configuration;
                TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
                // 获取类型处理器
                resultMapping.typeHandler = typeHandlerRegistry.getTypeHandler(resultMapping.javaType, null);
            }
        }

    }
}
