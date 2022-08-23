package com.ytrue.orm.mapping;

import com.ytrue.orm.reflection.MetaObject;
import com.ytrue.orm.session.Configuration;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ytrue
 * @date 2022/8/15 15:41
 * @description 绑定的SQL, 是从SqlSource而来，将动态内容都处理完成得到的SQL语句字符串，其中包括?,还有绑定的参数
 */
@Getter
@AllArgsConstructor
public class BoundSql {


    /**
     * sql语句
     */
    private String sql;

    /**
     * 参数映射 #{property,javaType=int,jdbcType=NUMERIC}
     */
    private List<ParameterMapping> parameterMappings;

    /**
     * 参数对象
     */
    private Object parameterObject;

    /**
     * 附加参数
     */
    private Map<String, Object> additionalParameters;


    /**
     * 参数的 MetaObject
     */
    private MetaObject metaParameters;

    public BoundSql(Configuration configuration, String sql, List<ParameterMapping> parameterMappings, Object parameterObject) {
        this.sql = sql;
        this.parameterMappings = parameterMappings;
        this.parameterObject = parameterObject;
        this.additionalParameters = new HashMap<>();
        this.metaParameters = configuration.newMetaObject(additionalParameters);
    }
}
