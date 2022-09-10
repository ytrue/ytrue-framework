package com.ytrue.orm.transaction;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author ytrue
 * @date 2022/8/15 14:51
 * @description Transaction
 */
public interface Transaction {

    /**
     * 获取连接
     *
     * @return
     * @throws SQLException
     */
    Connection getConnection() throws SQLException;

    /**
     * 事务提交
     *
     * @throws SQLException
     */
    void commit() throws SQLException;

    /**
     * 事务回滚
     *
     * @throws SQLException
     */
    void rollback() throws SQLException;

    /**
     * 关闭连接
     *
     * @throws SQLException
     */
    void close() throws SQLException;
}
