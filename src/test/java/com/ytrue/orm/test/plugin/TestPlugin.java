package com.ytrue.orm.test.plugin;

import com.ytrue.orm.executor.statement.StatementHandler;
import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.plugin.Interceptor;
import com.ytrue.orm.plugin.Intercepts;
import com.ytrue.orm.plugin.Invocation;
import com.ytrue.orm.plugin.Signature;

import java.sql.Connection;
import java.util.Properties;

/**
 * @author ytrue
 * @date 2022/9/6 17:28
 * @description TestPlugin
 */

@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class})})
public class TestPlugin implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取StatementHandler
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        // 获取SQL信息
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql();
        // 输出SQL
        System.out.println("拦截SQL：" + sql);
        // 放行
        return invocation.proceed();
    }

    @Override
    public void setProperties(Properties properties) {
        System.out.println("参数输出：" + properties.getProperty("test00"));
    }

}
