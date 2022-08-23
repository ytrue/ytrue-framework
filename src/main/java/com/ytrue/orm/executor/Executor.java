package com.ytrue.orm.executor;

import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.transaction.Transaction;
import com.ytrue.orm.session.ResultHandler;

import java.sql.SQLException;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/18 16:05
 * @description MyBatis执行器，是MyBatis调度的核心，负责SQL语句的生成和查询缓存的维护。
 */
public interface Executor {


    ResultHandler NO_RESULT_HANDLER = null;

    /**
     * 查询
     *
     * @param ms
     * @param parameter
     * @param resultHandler
     * @param boundSql
     * @param <E>
     * @return
     */
    <E> List<E> query(MappedStatement ms, Object parameter, ResultHandler resultHandler, BoundSql boundSql);

    /**
     * 获取事务管理器
     *
     * @return
     */
    Transaction getTransaction();

    /**
     * 提交事务
     *
     * @param required
     * @throws SQLException
     */
    void commit(boolean required) throws SQLException;

    /**
     * 回滚事务
     *
     * @param required
     * @throws SQLException
     */
    void rollback(boolean required) throws SQLException;

    /**
     * 关闭连接
     *
     * @param forceRollback
     */
    void close(boolean forceRollback);
}
