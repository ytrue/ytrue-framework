package com.ytrue.orm.scripting;

import com.ytrue.orm.executor.parameter.ParameterHandler;
import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.session.Configuration;
import org.dom4j.Element;

/**
 * @author ytrue
 * @date 2022/8/23 09:37
 * @description 脚本语言驱动
 * <p>
 * 在 XMLStatementBuilder#parseStatementNode 语句构建器的解析中，可以看到这么一块，
 * 获取默认语言驱动器并解析SQL的操作。其实这部分就是 XML 脚步语言驱动器所实现的功能，
 * 在 XMLScriptBuilder 中处理静态SQL和动态SQL
 * </p>
 */
public interface LanguageDriver {

    /**
     * 创建SqlSource
     *
     * @param configuration
     * @param script
     * @param parameterType
     * @return
     */
    SqlSource createSqlSource(Configuration configuration, Element script, Class<?> parameterType);

    /**
     * 创建参数处理器
     *
     * @param mappedStatement
     * @param parameterObject
     * @param boundSql
     * @return
     */
    ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);

}
