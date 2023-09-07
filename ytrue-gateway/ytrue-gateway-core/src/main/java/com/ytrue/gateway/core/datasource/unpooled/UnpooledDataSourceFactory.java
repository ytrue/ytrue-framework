package com.ytrue.gateway.core.datasource.unpooled;

import com.ytrue.gateway.core.datasource.DataSource;
import com.ytrue.gateway.core.datasource.DataSourceFactory;
import com.ytrue.gateway.core.datasource.DataSourceType;
import com.ytrue.gateway.core.session.Configuration;

/**
 * @author ytrue
 * @date 2023-09-07 9:29
 * @description UnpooledDataSourceFactory
 */
public class UnpooledDataSourceFactory implements DataSourceFactory {

    protected UnpooledDataSource dataSource;

    public UnpooledDataSourceFactory() {
        this.dataSource = new UnpooledDataSource();
    }

    @Override
    public void setProperties(Configuration configuration, String uri) {
        this.dataSource.setConfiguration(configuration);
        this.dataSource.setDataSourceType(DataSourceType.DUBBO);
        this.dataSource.setHttpStatement(configuration.getHttpStatement(uri));
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }
}
