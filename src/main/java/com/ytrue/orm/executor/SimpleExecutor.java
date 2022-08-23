package com.ytrue.orm.executor;

import com.ytrue.orm.executor.statement.StatementHandler;
import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.transaction.Transaction;
import com.ytrue.orm.session.ResultHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/18 16:24
 * @description SimpleExecutor
 */
public class SimpleExecutor extends BaseExecutor {

    public SimpleExecutor(Configuration configuration, Transaction transaction) {
        super(configuration, transaction);
    }

    /**
     * 执行方法
     *
     * @param ms
     * @param parameter
     * @param resultHandler
     * @param boundSql
     * @param <E>
     * @return
     */
    @Override
    protected <E> List<E> doQuery(MappedStatement ms, Object parameter, ResultHandler resultHandler, BoundSql boundSql) {
        try {
            Configuration configuration = ms.getConfiguration();
            StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, resultHandler, boundSql);
            Connection connection = transaction.getConnection();
            Statement stmt = handler.prepare(connection);
            handler.parameterize(stmt);
            Object query = handler.query(stmt, resultHandler);
            return (List<E>) query;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
