package com.ytrue.gateway.core.datasource;

/**
 * @author ytrue
 * @date 2023-09-07 9:23
 * @description 数据源接口，RPC、HTTP 都当做连接的数据资源使用
 */
public interface DataSource {

    /**
     * 获取连接
     *
     * @return
     */
    Connection getConnection();
}
