package com.ytrue.orm.scripting.defaults;

import com.alibaba.fastjson.JSON;
import com.ytrue.orm.executor.parameter.ParameterHandler;
import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.ParameterMapping;
import com.ytrue.orm.reflection.MetaObject;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.type.JdbcType;
import com.ytrue.orm.type.TypeHandler;
import com.ytrue.orm.type.TypeHandlerRegistry;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/24 14:55
 * @description 默认参数处理器
 */
@Slf4j
public class DefaultParameterHandler implements ParameterHandler {


    private final TypeHandlerRegistry typeHandlerRegistry;

    private final MappedStatement mappedStatement;
    private final Object parameterObject;
    private BoundSql boundSql;
    private Configuration configuration;


    public DefaultParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
        this.mappedStatement = mappedStatement;
        this.configuration = mappedStatement.getConfiguration();
        this.typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
        this.parameterObject = parameterObject;
        this.boundSql = boundSql;
    }

    @Override
    public Object getParameterObject() {
        return parameterObject;
    }

    @Override
    public void setParameters(PreparedStatement ps) throws SQLException {
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (null != parameterMappings) {
            for (int i = 0; i < parameterMappings.size(); i++) {
                // 根据index获取每一个
                ParameterMapping parameterMapping = parameterMappings.get(i);
                // 获取字段名称
                String propertyName = parameterMapping.getProperty();
                // 值
                Object value;

                // 如果参数类型在处理器有那么值就是直接赋值
                if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                    value = parameterObject;
                } else {
                    // 通过 MetaObject.getValue 反射取得值设进去
                    MetaObject metaObject = configuration.newMetaObject(parameterObject);
                    value = metaObject.getValue(propertyName);
                }


                JdbcType jdbcType = parameterMapping.getJdbcType();

                // 设置参数
                log.info("根据每个ParameterMapping中的TypeHandler设置对应的参数信息 value：{}", JSON.toJSONString(value));
                TypeHandler typeHandler = parameterMapping.getTypeHandler();
                // 对应的类型处理器设置参数
                typeHandler.setParameter(ps, i + 1, value, jdbcType);
            }
        }
    }
}
