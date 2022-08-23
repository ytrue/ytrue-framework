package com.ytrue.orm.datasource.pooled;

import com.ytrue.orm.datasource.unpooled.UnpooledDataSourceFactory;

import javax.sql.DataSource;

/**
 * @author ytrue
 * @date 2022/8/17 14:44
 * @description 有连接池的数据源工厂
 */
public class PooledDataSourceFactory extends UnpooledDataSourceFactory {

    public PooledDataSourceFactory() {
        this.dataSource = new PooledDataSource();
    }
}
