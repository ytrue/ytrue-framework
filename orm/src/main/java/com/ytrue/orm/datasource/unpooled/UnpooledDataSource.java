package com.ytrue.orm.datasource.unpooled;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @author ytrue
 * @date 2022/8/17 08:52
 * @description 无池化数据源实现
 * <p>
 * 这里的DataSource是一个数据源标准或者说规范，Java所有连接池需要基于这个规范进行实现
 */
@Getter
@Setter
public class UnpooledDataSource implements DataSource {

    private ClassLoader driverClassLoader;

    /**
     * 驱动配置，也可以扩展属性信息 driver.encoding=UTF8
     */
    private Properties driverProperties;

    /**
     * 驱动注册器
     */
    private static Map<String, Driver> registeredDrivers = new ConcurrentHashMap<>();

    /**
     * 驱动 mysql == com.mysql.jdbc.Drive
     */
    private String driver;
    /**
     * DB 链接地址
     */
    private String url;

    /**
     * 账号
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 是否自动提交
     */
    private Boolean autoCommit;

    /**
     * 事务级别
     */
    private Integer defaultTransactionIsolationLevel;

    static {
        // 获取全部已加载的驱动程序...
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        // 如果为true，就是有驱动
        while (drivers.hasMoreElements()) {
            // 获取驱动
            Driver driver = drivers.nextElement();
            /*
                驱动的类名为key
                这里会加载这么多驱动
                    com.alibaba.druid.proxy.DruidDriver
                    com.alibaba.druid.mock.MockDriver
                    com.mysql.fabric.jdbc.FabricMySQLDriver
                    com.mysql.jdbc.Driver
             */
            registeredDrivers.put(driver.getClass().getName(), driver);
        }
    }

    /**
     * 驱动代理
     */
    @AllArgsConstructor
    private static class DriverProxy implements Driver {

        private Driver driver;

        @Override
        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        @Override
        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return this.driver.getPropertyInfo(u, p);
        }

        @Override
        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        }
    }

    /**
     * 初始化驱动
     */
    private synchronized void initializerDriver() throws SQLException {
        // 如果不包含这个驱动，那么就注册
        if (!registeredDrivers.containsKey(driver)) {
            try {
                Class<?> driverType = Class.forName(driver, true, driverClassLoader);
                // https://www.kfu.com/~nsayer/Java/dyn-jdbc.html
                Driver driverInstance = (Driver) driverType.newInstance();
                DriverManager.registerDriver(new DriverProxy(driverInstance));
                registeredDrivers.put(driver, driverInstance);
            } catch (Exception e) {
                throw new SQLException("Error setting driver on UnpooledDataSource. Cause: " + e);
            }
        }
    }


    /**
     * 获取连接
     *
     * @param username
     * @param password
     * @return
     * @throws SQLException
     */
    private Connection doGetConnection(String username, String password) throws SQLException {
        Properties props = new Properties();
        if (driverProperties != null) {
            props.putAll(driverProperties);
        }
        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        return doGetConnection(props);
    }

    /**
     * 获取连接
     *
     * @param props
     * @return
     */
    private Connection doGetConnection(Properties props) throws SQLException {
        // 初始化驱动
        initializerDriver();
        // 通过DriverManager获取连接
        Connection connection = DriverManager.getConnection(url, props);
        // 设置自动提交
        if (autoCommit != null && autoCommit != connection.getAutoCommit()) {
            connection.setAutoCommit(autoCommit);
        }
        // 设置事务的隔离级别
        if (defaultTransactionIsolationLevel != null) {
            connection.setTransactionIsolation(defaultTransactionIsolationLevel);
        }
        return connection;
    }

    /**
     * 获取连接，主要实现方法
     *
     * @return
     * @throws SQLException
     */
    @Override
    public Connection getConnection() throws SQLException {
        return doGetConnection(username, password);
    }

    /**
     * 获取连接，主要实现方法
     *
     * @param username
     * @param password
     * @return
     * @throws SQLException
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return doGetConnection(username, password);
    }

    /**
     * 不重要
     *
     * @param iface
     * @param <T>
     * @return
     * @throws SQLException
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException(getClass().getName() + " is not a wrapper.");
    }

    /**
     * 不重要
     *
     * @param iface
     * @return
     * @throws SQLException
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    /**
     * 获取日志的输出
     *
     * @return
     * @throws SQLException
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    /**
     * 设置日志的输出
     *
     * @param out
     * @throws SQLException
     */
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        DriverManager.setLogWriter(out);
    }

    /**
     * 设置超时时间
     *
     * @param seconds
     * @throws SQLException
     */
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DriverManager.setLoginTimeout(seconds);
    }

    /**
     * 获取超时时间
     *
     * @return
     * @throws SQLException
     */
    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    /**
     * 获取日志
     *
     * @return
     * @throws SQLFeatureNotSupportedException
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }
}
