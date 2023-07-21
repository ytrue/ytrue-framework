package com.ytrue.orm.mapping;

import com.ytrue.orm.transaction.TransactionFactory;
import lombok.Builder;
import lombok.Getter;

import javax.sql.DataSource;

/**
 * @author ytrue
 * @date 2022/8/15 15:09
 * @description 环境
 */
@Getter
@Builder
public class Environment {

    /**
     * 环境
     */
    private final String id;

    /**
     * 事务工厂
     */
    private final TransactionFactory transactionFactory;

    /**
     * 数据源
     */
    private final DataSource dataSource;

    /**
     * 构造
     *
     * @param id
     * @param transactionFactory
     * @param dataSource
     */
    public Environment(String id, TransactionFactory transactionFactory, DataSource dataSource) {
        this.id = id;
        this.transactionFactory = transactionFactory;
        this.dataSource = dataSource;
    }
}
