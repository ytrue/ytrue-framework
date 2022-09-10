package com.ytrue.orm.builder;

import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.ParameterMapping;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.session.Configuration;

import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/23 14:20
 * @description StaticSqlSource
 */
public class StaticSqlSource implements SqlSource {

    private String sql;

    private List<ParameterMapping> parameterMappings;

    private Configuration configuration;

    public StaticSqlSource(Configuration configuration, String sql) {
        this(configuration, sql, null);
    }

    public StaticSqlSource(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
        this.sql = sql;
        this.parameterMappings = parameterMappings;
        this.configuration = configuration;
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        return new BoundSql(configuration, sql, parameterMappings, parameterObject);
    }
}
