package com.ytrue.orm.scripting.defaults;

import com.ytrue.orm.builder.SqlSourceBuilder;
import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.scripting.xmltags.DynamicContext;
import com.ytrue.orm.scripting.xmltags.SqlNode;
import com.ytrue.orm.session.Configuration;

import java.util.HashMap;

/**
 * @author ytrue
 * @date 2022/8/23 10:59
 * @description 原始SQL源码，比 DynamicSqlSource 动态SQL处理快
 */
public class RawSqlSource implements SqlSource {

    private final SqlSource sqlSource;

    public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
        this(configuration, getSql(configuration, rootSqlNode), parameterType);
    }

    public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
        SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
        Class<?> clazz = parameterType == null ? Object.class : parameterType;

        sqlSource = sqlSourceParser.parse(sql, clazz, new HashMap<>());
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        return sqlSource.getBoundSql(parameterObject);
    }

    /**
     * 获取 select 里面的sql
     *
     * @param configuration
     * @param rootSqlNode
     * @return
     */
    private static String getSql(Configuration configuration, SqlNode rootSqlNode) {
        DynamicContext context = new DynamicContext(configuration, null);
        rootSqlNode.apply(context);
        return context.getSql();
    }

}
