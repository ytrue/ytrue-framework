package com.ytrue.orm.executor.keygen;

import com.ytrue.orm.executor.Executor;
import com.ytrue.orm.mapping.MappedStatement;

import java.sql.Statement;

/**
 * @author ytrue
 * @date 2022/9/1 14:14
 * @description KeyGenerator
 */
public interface KeyGenerator {

    /**
     * 针对Sequence主键而言，在执行insert sql前必须指定一个主键值给要插入的记录，
     * 如Oracle、DB2，KeyGenerator提供了processBefore()方法。
     *
     * @param executor
     * @param ms
     * @param stmt
     * @param parameter
     */
    void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter);

    /**
     * 针对自增主键的表，在插入时不需要主键，而是在插入过程自动获取一个自增的主键，
     * 比如MySQL、PostgreSQL，KeyGenerator提供了processAfter()方法
     *
     * @param executor
     * @param ms
     * @param stmt
     * @param parameter
     */
    void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter);
}
