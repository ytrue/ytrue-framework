package com.ytrue.gateway.core.datasource;

import com.ytrue.gateway.core.session.Configuration;

/**
 * @author ytrue
 * @date 2023-09-07 9:24
 * @description 数据源工厂
 */
public interface DataSourceFactory {


    /**
     * 设置配置
     *
     * @param configuration
     * @param uri
     */
    void setProperties(Configuration configuration, String uri);

    /**
     * 获取数据源
     *
     * @return
     */
    DataSource getDataSource();
}
