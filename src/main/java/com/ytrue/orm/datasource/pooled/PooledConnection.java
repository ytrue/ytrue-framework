package com.ytrue.orm.datasource.pooled;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author ytrue
 * @date 2022/8/17 14:44
 * @description 池化代理的链接，这里生成代理连接，在连接关闭的时候把连接放入到连接池中
 */
@Slf4j
public class PooledConnection implements InvocationHandler {

    /**
     * 关闭方法的常量
     */
    private static final String CLOSE = "close";

    /**
     * jdk动态代理传递的参数，我们要对Connection 的 close方法进行增强处理，把连接放入连接池中
     */
    private static final Class<?>[] IFACES = new Class<?>[]{Connection.class};

    /**
     * hasCode
     */
    private int hashCode;


    /**
     * 池化的数据源
     */
    private PooledDataSource dataSource;

    /**
     * 真实的连接
     */
    private Connection realConnection;

    /**
     * 代理的连接
     */
    private Connection proxyConnection;

    /**
     * 等待时间戳
     */
    private long checkoutTimestamp;

    /**
     * 创建时间戳
     */
    private long createdTimestamp;

    /**
     * 最后使用时间戳
     */
    private long lastUsedTimestamp;

    /**
     * 连接类型code
     */
    private int connectionTypeCode;

    /**
     * 是否有效
     */
    private boolean valid;


    public PooledConnection(Connection connection, PooledDataSource dataSource) {
        this.hashCode = connection.hashCode();
        this.realConnection = connection;
        this.dataSource = dataSource;
        this.createdTimestamp = System.currentTimeMillis();
        this.lastUsedTimestamp = System.currentTimeMillis();
        this.valid = true;

        // 创建代理连接
        this.proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), IFACES, this);
    }

    /**
     * 增强方法
     *
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 获取执行的方法
        String methodName = method.getName();
        // 如果是调用 close 方法，则将链接加入连接池中，并返回null
        if (CLOSE.hashCode() == methodName.hashCode() && CLOSE.equals(methodName)) {
            // 加入连接池中
            dataSource.pushConnection(this);
            return null;
        } else {
            // 如果不是 object的方法 就进行检查
            if (!Object.class.equals(method.getDeclaringClass())) {
                // 除了toString()方法，其他方法调用之前要检查connection是否还是合法的,不合法要抛出SQLException
                checkConnection();
            }

            // 其他方法交给connection去调用
            return method.invoke(realConnection, args);
        }
    }

    /**
     * 检查是否有效
     *
     * @throws SQLException
     */
    private void checkConnection() throws SQLException {
        if (!valid) {
            throw new SQLException("Error accessing PooledConnection. Connection is invalid.");
        }
    }


    /**
     * 无效
     */
    public void invalidate() {
        valid = false;
    }

    /**
     * 使用有效
     *
     * @return
     */
    public boolean isValid() {
        return valid && realConnection != null && dataSource.pingConnection(this);
    }


    public Connection getRealConnection() {
        return realConnection;
    }

    public Connection getProxyConnection() {
        return proxyConnection;
    }

    public int getRealHashCode() {
        return realConnection == null ? 0 : realConnection.hashCode();
    }

    public int getConnectionTypeCode() {
        return connectionTypeCode;
    }

    public void setConnectionTypeCode(int connectionTypeCode) {
        this.connectionTypeCode = connectionTypeCode;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public long getLastUsedTimestamp() {
        return lastUsedTimestamp;
    }

    public void setLastUsedTimestamp(long lastUsedTimestamp) {
        this.lastUsedTimestamp = lastUsedTimestamp;
    }

    public long getTimeElapsedSinceLastUse() {
        return System.currentTimeMillis() - lastUsedTimestamp;
    }

    public long getAge() {
        return System.currentTimeMillis() - createdTimestamp;
    }

    public long getCheckoutTimestamp() {
        return checkoutTimestamp;
    }

    public void setCheckoutTimestamp(long timestamp) {
        this.checkoutTimestamp = timestamp;
    }

    public long getCheckoutTime() {
        return System.currentTimeMillis() - checkoutTimestamp;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PooledConnection) {
            return realConnection.hashCode() == (((PooledConnection) obj).realConnection.hashCode());
        } else if (obj instanceof Connection) {
            return hashCode == obj.hashCode();
        } else {
            return false;
        }
    }
}
