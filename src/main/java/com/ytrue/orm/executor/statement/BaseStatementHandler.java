package com.ytrue.orm.executor.statement;

import com.ytrue.orm.executor.Executor;
import com.ytrue.orm.executor.parameter.ParameterHandler;
import com.ytrue.orm.executor.resultset.ResultSetHandler;
import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.ResultHandler;
import com.ytrue.orm.session.RowBounds;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author ytrue
 * @date 2022/8/18 16:40
 * @description BaseStatementHandler
 */
public abstract class BaseStatementHandler implements StatementHandler {
    protected final Configuration configuration;
    protected final Executor executor;
    protected final MappedStatement mappedStatement;

    protected final Object parameterObject;
    protected final ResultSetHandler resultSetHandler;
    protected final ParameterHandler parameterHandler;

    protected final RowBounds rowBounds;
    protected BoundSql boundSql;

    public BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        this.configuration = mappedStatement.getConfiguration();
        this.executor = executor;
        this.mappedStatement = mappedStatement;
        this.rowBounds = rowBounds;
        this.boundSql = boundSql;

        this.parameterObject = parameterObject;
        this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
        this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, resultHandler, boundSql);
    }


    @Override
    public Statement prepare(Connection connection) throws SQLException {
        Statement statement;
        try {
            // 实例化 Statement
            statement = instantiateStatement(connection);
            // 参数设置，可以被抽取，提供配置
            statement.setQueryTimeout(350);
            statement.setFetchSize(10000);
            return statement;
        } catch (Exception e) {
            throw new RuntimeException("Error preparing statement.  Cause: " + e, e);
        }
    }

    /**
     * 实例化一个 Statement
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    protected abstract Statement instantiateStatement(Connection connection) throws SQLException;
}