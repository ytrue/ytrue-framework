package com.ytrue.orm.scripting.xmltags;

import com.ytrue.orm.executor.parameter.ParameterHandler;
import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.scripting.LanguageDriver;
import com.ytrue.orm.scripting.defaults.DefaultParameterHandler;
import com.ytrue.orm.scripting.defaults.RawSqlSource;
import com.ytrue.orm.session.Configuration;
import org.dom4j.Element;

/**
 * @author ytrue
 * @date 2022/8/23 09:42
 * @description XML语言驱动器
 */
public class XMLLanguageDriver implements LanguageDriver {

    @Override
    public SqlSource createSqlSource(Configuration configuration, Element script, Class<?> parameterType) {
        // 用XML脚本构建器解析  script == select，parameterType == 参数类型
        XMLScriptBuilder builder = new XMLScriptBuilder(configuration, script, parameterType);
        return builder.parseScriptNode();
    }

    /**
     * step-12 新增方法，用于处理注解配置 SQL 语句
     */
    @Override
    public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
        // 暂时不解析动态 SQL
        return new RawSqlSource(configuration, script, parameterType);
    }

    @Override
    public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
        return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
    }
}
