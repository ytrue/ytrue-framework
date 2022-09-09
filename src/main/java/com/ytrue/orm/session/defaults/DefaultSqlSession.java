package com.ytrue.orm.session.defaults;

import com.alibaba.fastjson.JSON;
import com.ytrue.orm.executor.Executor;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.RowBounds;
import com.ytrue.orm.session.SqlSession;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/11 15:25
 * @description 默认SqlSession实现类
 */
@Slf4j
public class DefaultSqlSession implements SqlSession {

    private Configuration configuration;
    private Executor executor;

    public DefaultSqlSession(Configuration configuration, Executor executor) {
        this.configuration = configuration;
        this.executor = executor;
    }

    @Override
    public <T> T selectOne(String statement) {
        return this.selectOne(statement, null);
    }

    @Override
    public <T> T selectOne(String statement, Object parameter) {
        // 取一条即可，如果存在多条抛出异常
        List<T> list = this.selectList(statement, parameter);
        if (list.size() == 1) {
            return list.get(0);
        } else if (list.size() > 1) {
            throw new RuntimeException("Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
        } else {
            return null;
        }
    }


    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        log.debug("执行查询 statement：{} parameter：{}", statement, JSON.toJSONString(parameter));
        MappedStatement ms = configuration.getMappedStatement(statement);
        try {
            return executor.query(ms, parameter, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER);
        } catch (SQLException e) {
            throw new RuntimeException("Error querying database.  Cause: " + e);
        }
    }

    @Override
    public int insert(String statement, Object parameter) {
        // 在 Mybatis 中 insert 调用的是 update
        return update(statement, parameter);
    }

    @Override
    public int update(String statement, Object parameter) {
        MappedStatement ms = configuration.getMappedStatement(statement);
        try {
            return executor.update(ms, parameter);
        } catch (SQLException e) {
            throw new RuntimeException("Error updating database.  Cause: " + e);
        }
    }

    @Override
    public Object delete(String statement, Object parameter) {
        return update(statement, parameter);
    }

    @Override
    public void commit() {
        try {
            executor.commit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Error committing transaction.  Cause: " + e);
        }
    }

    @Override
    public void close() {
        executor.close(false);
    }

    @Override
    public void clearCache() {
        executor.clearLocalCache();
    }

    /**
     * 获取传入的class 的代理对象
     *
     * @param type Mapper interface class
     * @param <T>
     * @return
     */
    @Override
    public <T> T getMapper(Class<T> type) {
        return configuration.getMapper(type, this);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }
}
