package com.ytrue.orm.transaction.jdbc;

import com.ytrue.orm.session.TransactionIsolationLevel;
import com.ytrue.orm.transaction.Transaction;
import com.ytrue.orm.transaction.TransactionFactory;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author ytrue
 * @date 2022/8/15 15:03
 * @description JdbcTransaction 工厂
 */
public class JdbcTransactionFactory implements TransactionFactory {

    @Override
    public Transaction newTransaction(Connection conn) {
        return new JdbcTransaction(conn);
    }

    @Override
    public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
        return new JdbcTransaction(dataSource, level, autoCommit);
    }
}
