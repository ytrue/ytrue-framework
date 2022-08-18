package com.ytrue.orm.transaction.jdbc;

import com.ytrue.orm.session.TransactionIsolationLevel;
import com.ytrue.orm.transaction.Transaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author ytrue
 * @date 2022/8/15 14:58
 * @description JDBC 事务，直接利用 JDBC 的 commit、rollback。依赖于数据源获得的连接来管理事务范围。
 */
public class JdbcTransaction implements Transaction {

    /**
     * 连接
     */
    protected Connection connection;

    /**
     * 数据源
     */
    protected DataSource dataSource;

    /**
     * 事务的隔离级别
     */
    protected TransactionIsolationLevel level = TransactionIsolationLevel.NONE;

    /**
     * 事务自动提交
     */
    protected boolean autoCommit;


    /**
     * 构造方法
     *
     * @param connection
     */
    public JdbcTransaction(Connection connection) {
        this.connection = connection;
    }

    /**
     * 构造方法
     *
     * @param dataSource
     * @param level
     * @param autoCommit
     */
    public JdbcTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
        this.dataSource = dataSource;
        this.level = level;
        this.autoCommit = autoCommit;
    }

    /**
     * 获取连接
     *
     * @return
     * @throws SQLException
     */
    @Override
    public Connection getConnection() throws SQLException {
        // 获取连接
        connection = dataSource.getConnection();
        // 设置事务级别
        connection.setTransactionIsolation(level.getLevel());
        // 设置事务是否自动提交
        connection.setAutoCommit(autoCommit);
        // 返回连接
        return connection;
    }

    /**
     * 提交事务
     *
     * @throws SQLException
     */
    @Override
    public void commit() throws SQLException {
        if (connection != null && !connection.getAutoCommit()) {
            connection.commit();
        }
    }

    /**
     * 回滚事务
     *
     * @throws SQLException
     */
    @Override
    public void rollback() throws SQLException {
        if (connection != null && !connection.getAutoCommit()) {
            connection.rollback();
        }
    }


    /**
     * 关闭连接
     *
     * @throws SQLException
     */
    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.getAutoCommit()) {
            connection.close();
        }
    }
}
