package com.ytrue.orm.executor;

import com.ytrue.orm.cache.CacheKey;
import com.ytrue.orm.cache.impl.PerpetualCache;
import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.ParameterMapping;
import com.ytrue.orm.reflection.MetaObject;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.LocalCacheScope;
import com.ytrue.orm.session.ResultHandler;
import com.ytrue.orm.session.RowBounds;
import com.ytrue.orm.transaction.Transaction;
import com.ytrue.orm.type.TypeHandlerRegistry;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/18 16:08
 * @description 执行器抽象基类
 */
@Slf4j
public abstract class BaseExecutor implements Executor {

    protected Configuration configuration;
    protected Transaction transaction;
    protected Executor wrapper;

    /**
     * 事务是否关闭
     */
    private boolean closed;


    /**
     * 本地缓存
     */
    protected PerpetualCache localCache;

    /**
     * 查询堆栈
     */
    protected int queryStack = 0;

    protected BaseExecutor(Configuration configuration, Transaction transaction) {
        this.configuration = configuration;
        this.transaction = transaction;
        this.wrapper = this;

        this.localCache = new PerpetualCache("LocalCache");
    }

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
        BoundSql boundSql = ms.getBoundSql(parameter);
        // 2. 创建缓存Key
        CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
        return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
    }

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
        if (closed) {
            throw new RuntimeException("Executor was closed.");
        }

        // 清理局部缓存，查询堆栈为0则清理。queryStack 避免递归调用清理
        if (queryStack == 0 && ms.isFlushCacheRequired()) {
            clearLocalCache();
        }
        List<E> list;
        try {
            queryStack++;
            // 根据cacheKey从localCache中查询数据
            list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
            if (list == null) {
                // 数据库查询
                list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
            }
        } finally {
            queryStack--;
        }
        if (queryStack == 0) {
            if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
                clearLocalCache();
            }
        }
        return list;
    }

    /**
     * 数据库查询
     * @param ms
     * @param parameter
     * @param rowBounds
     * @param resultHandler
     * @param key
     * @param boundSql
     * @param <E>
     * @return
     * @throws SQLException
     */
    private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
        List<E> list;
        localCache.putObject(key, ExecutionPlaceholder.EXECUTION_PLACEHOLDER);
        try {
            list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
        } finally {
            localCache.removeObject(key);
        }
        // 存入缓存
        localCache.putObject(key, list);
        return list;
    }

    /**
     * 真正执行的方法
     *
     * @param ms
     * @param parameter
     * @param rowBounds
     * @param resultHandler
     * @param boundSql
     * @param <E>
     * @return
     * @throws SQLException
     */
    protected abstract <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException;


    @Override
    public Transaction getTransaction() {
        if (closed) {
            throw new RuntimeException("Executor was closed.");
        }
        return transaction;
    }

    @Override
    public void commit(boolean required) throws SQLException {
        if (closed) {
            throw new RuntimeException("Cannot commit, transaction is already closed");
        }
        if (required) {
            transaction.commit();
        }
    }

    @Override
    public void rollback(boolean required) throws SQLException {
        if (!closed) {
            if (required) {
                transaction.rollback();
            }
        }
    }

    @Override
    public void close(boolean forceRollback) {
        try {
            try {
                rollback(forceRollback);
            } finally {
                transaction.close();
            }
        } catch (SQLException e) {
            log.warn("Unexpected exception on closing transaction.  Cause: " + e);
        } finally {
            // 把事务管理器设置成null
            transaction = null;
            // 关闭
            closed = true;
        }
    }


    @Override
    public int update(MappedStatement ms, Object parameter) throws SQLException {
        return doUpdate(ms, parameter);
    }

    /**
     * 更新
     *
     * @param ms
     * @param parameter
     * @return
     * @throws SQLException
     */
    protected abstract int doUpdate(MappedStatement ms, Object parameter) throws SQLException;


    /**
     * 关闭 Statement
     *
     * @param statement
     */
    protected void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignore) {
            }
        }
    }


    @Override
    public void clearLocalCache() {
        if (!closed) {
            localCache.clear();
        }
    }

    @Override
    public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
        if (closed) {
            throw new RuntimeException("Executor was closed.");
        }

        CacheKey cacheKey = new CacheKey();
        cacheKey.update(ms.getId());
        cacheKey.update(rowBounds.getOffset());
        cacheKey.update(rowBounds.getLimit());
        cacheKey.update(boundSql.getSql());

        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();

        for (ParameterMapping parameterMapping : parameterMappings) {
            Object value;
            String propertyName = parameterMapping.getProperty();
            if (boundSql.hasAdditionalParameter(propertyName)) {
                value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
                value = null;
            } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                value = parameterObject;
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                value = metaObject.getValue(propertyName);
            }
            cacheKey.update(value);
        }
        if (configuration.getEnvironment() != null) {
            cacheKey.update(configuration.getEnvironment().getId());
        }
        return cacheKey;
    }
}
