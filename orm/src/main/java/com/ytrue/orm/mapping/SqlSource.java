package com.ytrue.orm.mapping;

/**
 * @author ytrue
 * @date 2022/8/23 09:40
 * @description SQL源码
 */
public interface SqlSource {

    /**
     * 获取 BoundSql
     *
     * @param parameterObject
     * @return
     */
    BoundSql getBoundSql(Object parameterObject);
}
