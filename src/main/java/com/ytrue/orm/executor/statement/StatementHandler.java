package com.ytrue.orm.executor.statement;

import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.session.ResultHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/18 16:38
 * @description 语句处理器, 封装了JDBC Statement操作，负责对JDBC Statement的操作，如设置参数等。
 */
public interface StatementHandler {

    /**
     * 准备语句
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    Statement prepare(Connection connection) throws SQLException;

    /**
     * 参数化
     *
     * @param statement
     * @throws SQLException
     */
    void parameterize(Statement statement) throws SQLException;

    /**
     * 执行sql
     *
     * @param statement
     * @param resultHandler
     * @param <E>
     * @return
     * @throws SQLException
     */
    <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException;


    /**
     * 执行更新
     *
     * @param statement
     * @return
     * @throws SQLException
     */
    int update(Statement statement) throws SQLException;


    /**
     * 获取绑定SQL
     * @return
     */
    BoundSql getBoundSql();
}
