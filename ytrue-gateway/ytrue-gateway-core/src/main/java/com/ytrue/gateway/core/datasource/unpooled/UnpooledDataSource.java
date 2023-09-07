package com.ytrue.gateway.core.datasource.unpooled;

import com.ytrue.gateway.core.datasource.Connection;
import com.ytrue.gateway.core.datasource.DataSource;
import com.ytrue.gateway.core.datasource.DataSourceType;
import com.ytrue.gateway.core.datasource.connection.DubboConnection;
import com.ytrue.gateway.core.mapping.HttpStatement;
import com.ytrue.gateway.core.session.Configuration;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.service.GenericService;

/**
 * @author ytrue
 * @date 2023-09-07 9:29
 * @description 无池化的连接池
 */
public class UnpooledDataSource implements DataSource {


    private Configuration configuration;
    private HttpStatement httpStatement;
    private DataSourceType dataSourceType;

    public UnpooledDataSource() {
    }

    public UnpooledDataSource(Configuration configuration, HttpStatement httpStatement, DataSourceType dataSourceType) {
        this.configuration = configuration;
        this.httpStatement = httpStatement;
        this.dataSourceType = dataSourceType;
    }

    @Override
    public Connection getConnection() {
        switch (dataSourceType) {
            case HTTP:
                // TODO 预留接口，暂时不需要实现
                break;
            case DUBBO:
                // 配置信息
                String application = httpStatement.getApplication();
                String interfaceName = httpStatement.getInterfaceName();
                // 获取服务
                ApplicationConfig applicationConfig = configuration.getApplicationConfig(application);
                RegistryConfig registryConfig = configuration.getRegistryConfig(application);
                ReferenceConfig<GenericService> reference = configuration.getReferenceConfig(interfaceName);
                return new DubboConnection(applicationConfig, registryConfig, reference);
            default:
                break;
        }
        throw new RuntimeException("DataSourceType：" + dataSourceType + "没有对应的数据源实现");
    }


    // ------------------------ set
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setHttpStatement(HttpStatement httpStatement) {
        this.httpStatement = httpStatement;
    }

    public void setDataSourceType(DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }
}
