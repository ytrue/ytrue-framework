package com.ytrue.orm.session;

import java.sql.Connection;

/**
 * @author ytrue
 * @date 2022/8/15 14:55
 * @description TransactionIsolationLevel
 */
public enum TransactionIsolationLevel {

    /**
     * 不支持事务
     */
    NONE(Connection.TRANSACTION_NONE),

    /**
     * 读未提交
     */
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),

    /**
     * 读提交
     */
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),

    /**
     * 可重复读
     */
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),

    /**
     * 可序列化
     */
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    private final int level;

    TransactionIsolationLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
