package com.ytrue.orm.datasource.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.ytrue.orm.datasource.DataSourceFactory;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author ytrue
 * @date 2022/8/15 15:26
 * @description Druid 数据源工厂
 */
public class DruidDataSourceFactory implements DataSourceFactory {

    private Properties props;

    @Override
    public void setProperties(Properties props) {
        this.props = props;

    }

    @Override
    public DataSource getDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(props.getProperty("driver"));
        dataSource.setUrl(props.getProperty("url"));
        dataSource.setUsername(props.getProperty("username"));
        dataSource.setPassword(props.getProperty("password"));
        return dataSource;
    }
}
