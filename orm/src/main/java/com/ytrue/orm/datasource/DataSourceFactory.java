package com.ytrue.orm.datasource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author ytrue
 * @date 2022/8/15 15:25
 * @description DataSourceFactory
 */
public interface DataSourceFactory {

    /**
     * 设置
     *
     * @param props
     */
    void setProperties(Properties props);

    /**
     * 获取数据源
     *
     * @return
     */
    DataSource getDataSource();
}
