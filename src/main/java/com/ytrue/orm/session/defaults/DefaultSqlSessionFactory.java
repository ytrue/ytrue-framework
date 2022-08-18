package com.ytrue.orm.session.defaults;

import com.ytrue.orm.executor.Executor;
import com.ytrue.orm.mapping.Environment;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.SqlSession;
import com.ytrue.orm.session.SqlSessionFactory;
import com.ytrue.orm.session.TransactionIsolationLevel;
import com.ytrue.orm.transaction.Transaction;
import com.ytrue.orm.transaction.TransactionFactory;

import java.sql.SQLException;

/**
 * @author ytrue
 * @date 2022/8/11 15:26
 * @description 默认的 DefaultSqlSessionFactory
 */
public class DefaultSqlSessionFactory implements SqlSessionFactory {

    private final Configuration configuration;

    public DefaultSqlSessionFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public SqlSession openSession() {
        Transaction tx = null;
        try {
            final Environment environment = configuration.getEnvironment();
            TransactionFactory transactionFactory = environment.getTransactionFactory();
            tx = transactionFactory.newTransaction(configuration.getEnvironment().getDataSource(), TransactionIsolationLevel.READ_COMMITTED, false);
            // 创建执行器
            final Executor executor = configuration.newExecutor(tx);
            // 创建DefaultSqlSession
            return new DefaultSqlSession(configuration, executor);
        } catch (Exception e) {
            try {
                assert tx != null;
                tx.close();
            } catch (SQLException ignore) {
            }
            throw new RuntimeException("Error opening session.  Cause: " + e);
        }
    }

}
