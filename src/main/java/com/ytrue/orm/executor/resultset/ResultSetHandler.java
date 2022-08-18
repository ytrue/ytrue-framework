package com.ytrue.orm.executor.resultset;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/18 16:41
 * @description ResultSetHandler
 */
public interface ResultSetHandler {
    /**
     * 处理结果
     *
     * @param ps
     * @param <E>
     * @return
     * @throws SQLException
     */
    <E> List<E> handleResultSets(Statement ps) throws SQLException;
}
