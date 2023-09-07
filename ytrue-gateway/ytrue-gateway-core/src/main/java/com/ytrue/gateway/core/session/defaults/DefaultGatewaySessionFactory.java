package com.ytrue.gateway.core.session.defaults;

import com.ytrue.gateway.core.datasource.DataSource;
import com.ytrue.gateway.core.datasource.DataSourceFactory;
import com.ytrue.gateway.core.datasource.unpooled.UnpooledDataSourceFactory;
import com.ytrue.gateway.core.executor.Executor;
import com.ytrue.gateway.core.session.Configuration;
import com.ytrue.gateway.core.session.GatewaySession;
import com.ytrue.gateway.core.session.GatewaySessionFactory;

/**
 * @author ytrue
 * @date 2023-09-06 17:02
 * @description DefaultGatewaySessionFactory
 */
public class DefaultGatewaySessionFactory implements GatewaySessionFactory {

    private final Configuration configuration;

    public DefaultGatewaySessionFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public GatewaySession openSession(String uri) {
        // 获取数据源连接信息：这里把 Dubbo、HTTP 抽象为一种连接资源
        DataSourceFactory dataSourceFactory = new UnpooledDataSourceFactory();
        dataSourceFactory.setProperties(configuration, uri);
        DataSource dataSource = dataSourceFactory.getDataSource();
        // 创建执行器
        Executor executor = configuration.newExecutor(dataSource.getConnection());

        return new DefaultGatewaySession(configuration, uri, executor);
    }

}
