package com.ytrue.orm.datasource.pooled;

import com.ytrue.orm.datasource.unpooled.UnpooledDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.logging.Logger;

/**
 * @author ytrue
 * @date 2022/8/17 14:44
 * @description PooledDataSource
 */
@Slf4j
public class PooledDataSource implements DataSource {

    /**
     * 池状态
     */
    private final PoolState state = new PoolState(this);

    /**
     * 无池化的数据源
     */
    private final UnpooledDataSource dataSource;

    /**
     * 活跃连接数
     */
    protected int poolMaximumActiveConnections = 10;

    /**
     * 空闲连接数
     */
    protected int poolMaximumIdleConnections = 5;

    /**
     * 在被强制返回之前,池中连接被检查的时间
     */
    protected int poolMaximumCheckoutTime = 20000;

    /**
     * 这是给连接池一个打印日志状态机会的低层次设置,还有重新尝试获得连接, 这些情况下往往需要很长时间 为了避免连接池没有配置时静默失败)。
     */
    protected int poolTimeToWait = 20000;

    /**
     * 发送到数据的侦测查询,用来验证连接是否正常工作,并且准备 接受请求。默认是“NO PING QUERY SET” ,这会引起许多数据库驱动连接由一 个错误信息而导致失败
     */
    protected String poolPingQuery = "NO PING QUERY SET";

    /**
     * 开启或禁用侦测查询
     */
    protected boolean poolPingEnabled = false;

    /**
     * 用来配置 poolPingQuery 多次时间被用一次
     */
    protected int poolPingConnectionsNotUsedFor = 0;


    /**
     * 预期的连接类型代码
     */
    private int expectedConnectionTypeCode;


    public PooledDataSource() {
        this.dataSource = new UnpooledDataSource();
    }

    /**
     * 放入连接
     *
     * @param connection
     * @throws SQLException
     */
    protected void pushConnection(PooledConnection connection) throws SQLException {
        synchronized (state) {
            // 从活跃的list中移除
            state.activeConnections.remove(connection);
            // 判断链接是否有效
            if (connection.isValid()) {
                // 如果空闲链接小于设定数量，也就是太少时
                if (state.idleConnections.size() < poolMaximumIdleConnections &&
                        connection.getConnectionTypeCode() == expectedConnectionTypeCode
                ) {
                    // 追加检查的总时间
                    state.accumulatedCheckoutTime += connection.getCheckoutTime();

                    // 如果真实连接不是自动提交，这里connection回滚
                    if (!connection.getRealConnection().getAutoCommit()) {
                        // 回滚
                        connection.getRealConnection().rollback();
                    }

                    // 实例化一个新的DB连接，加入到idle列表
                    PooledConnection newConnection = new PooledConnection(connection.getRealConnection(), this);
                    state.idleConnections.add(newConnection);
                    newConnection.setCreatedTimestamp(connection.getCreatedTimestamp());
                    newConnection.setLastUsedTimestamp(connection.getLastUsedTimestamp());

                    // 代理连接设置无效
                    connection.invalidate();
                    log.info("Returned connection " + newConnection.getRealHashCode() + " to pool.");
                    // 通知其他线程可以来抢DB连接了
                    state.notifyAll();
                } else {
                    // 否则，空闲链接还比较充足
                    state.accumulatedCheckoutTime += connection.getCheckoutTime();

                    if (!connection.getRealConnection().getAutoCommit()) {
                        connection.getRealConnection().rollback();
                    }
                    // 将connection关闭
                    connection.getRealConnection().close();
                    log.info("Closed connection " + connection.getRealHashCode() + ".");
                    // 设置无效
                    connection.invalidate();
                }
            } else {
                log.info("A bad connection (" + connection.getRealHashCode() + ") attempted to return to the pool, discarding connection.");
                state.badConnectionCount++;
            }
        }
    }


    /**
     * 弹出连接
     *
     * @param username
     * @param password
     * @return
     * @throws SQLException
     */
    private PooledConnection popConnection(String username, String password) throws SQLException {
        boolean countedWait = false;
        PooledConnection conn = null;
        long t = System.currentTimeMillis();

        int localBadConnectionCount = 0;

        while (conn == null) {
            synchronized (state) {
                // 如果有空闲链接：返回第一个
                if (!state.idleConnections.isEmpty()) {
                    conn = state.idleConnections.remove(0);
                    log.info("Checked out connection " + conn.getRealHashCode() + " from pool.");
                } else {
                    // 如果无空闲链接：创建新的链接

                    // 活跃连接数不足
                    if (state.activeConnections.size() < poolMaximumActiveConnections) {
                        conn = new PooledConnection(dataSource.getConnection(), this);
                        log.info("Created connection " + conn.getRealHashCode() + ".");
                    }
                    // 活跃连接数已满
                    else {
                        // 取得活跃链接列表的第一个，也就是最老的一个连接
                        PooledConnection oldestActiveConnection = state.activeConnections.get(0);
                        long longestCheckoutTime = oldestActiveConnection.getCheckoutTime();
                        // 如果checkout时间过长，则这个链接标记为过期，删除掉，再创建一个新的连接
                        if (longestCheckoutTime > poolMaximumCheckoutTime) {
                            state.claimedOverdueConnectionCount++;
                            state.accumulatedCheckoutTimeOfOverdueConnections += longestCheckoutTime;
                            state.accumulatedCheckoutTime += longestCheckoutTime;
                            // 移除
                            state.activeConnections.remove(oldestActiveConnection);

                            if (!oldestActiveConnection.getRealConnection().getAutoCommit()) {
                                oldestActiveConnection.getRealConnection().rollback();
                            }
                            // 删掉最老的链接，然后重新实例化一个新的链接
                            conn = new PooledConnection(oldestActiveConnection.getRealConnection(), this);
                            // 设置旧的来连接为无效
                            oldestActiveConnection.invalidate();
                            log.info("Claimed overdue connection " + conn.getRealHashCode() + ".");
                        } else {
                            // 如果checkout超时时间不够长，则等待
                            try {
                                if (!countedWait) {
                                    state.hadToWaitCount++;
                                    countedWait = true;
                                }
                                log.info("Waiting as long as " + poolTimeToWait + " milliseconds for connection.");
                                long wt = System.currentTimeMillis();
                                state.wait(poolTimeToWait);
                                state.accumulatedWaitTime += System.currentTimeMillis() - wt;
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                }

                // 获得到链接
                if (conn != null) {
                    // 判断连接是否有效
                    if (conn.isValid()) {
                        if (!conn.getRealConnection().getAutoCommit()) {
                            conn.getRealConnection().rollback();
                        }
                        conn.setConnectionTypeCode(assembleConnectionTypeCode(dataSource.getUrl(), username, password));

                        // 记录checkout时间
                        conn.setCheckoutTimestamp(System.currentTimeMillis());
                        conn.setLastUsedTimestamp(System.currentTimeMillis());
                        state.activeConnections.add(conn);
                        state.requestCount++;
                        state.accumulatedRequestTime += System.currentTimeMillis() - t;
                    } else {
                        log.info("A bad connection (" + conn.getRealHashCode() + ") was returned from the pool, getting another connection.");
                        // 如果没拿到，统计信息：失败链接 +1
                        state.badConnectionCount++;
                        localBadConnectionCount++;
                        conn = null;
                        // 失败次数较多，抛异常
                        if (localBadConnectionCount > (poolMaximumIdleConnections + 3)) {
                            log.debug("PooledDataSource: Could not get a good connection to the database.");
                            throw new SQLException("PooledDataSource: Could not get a good connection to the database.");
                        }
                    }
                }
            }
        }
        // 如果连接是null 这里要抛出异常
        if (conn == null) {
            log.debug("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
            throw new SQLException("PooledDataSource: Unknown severe error condition.  The connection pool returned a null connection.");
        }
        return conn;
    }

    /**
     * 关闭所有的连接
     */
    public void forceCloseAll() {
        synchronized (state) {
            expectedConnectionTypeCode = assembleConnectionTypeCode(dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword());
            // 关闭活跃链接
            for (int i = state.activeConnections.size(); i > 0; i--) {
                try {
                    PooledConnection conn = state.activeConnections.remove(i - 1);
                    conn.invalidate();

                    Connection realConn = conn.getRealConnection();
                    if (!realConn.getAutoCommit()) {
                        realConn.rollback();
                    }
                    realConn.close();
                } catch (Exception ignore) {

                }
            }

            // 关闭空闲链接
            for (int i = state.idleConnections.size(); i > 0; i--) {
                try {
                    PooledConnection conn = state.idleConnections.remove(i - 1);
                    conn.invalidate();

                    Connection realConn = conn.getRealConnection();
                    if (!realConn.getAutoCommit()) {
                        realConn.rollback();
                    }
                } catch (Exception ignore) {

                }
            }

            log.info("PooledDataSource forcefully closed/removed all connections.");
        }
    }

    /**
     * ping Connection 是否还活着
     *
     * @param conn
     * @return
     */
    protected boolean pingConnection(PooledConnection conn) {
        boolean result;

        try {
            // 判断是否关闭了
            result = !conn.getRealConnection().isClosed();
        } catch (SQLException e) {
            log.info("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
            result = false;
        }

        // 如果连接是没有关闭的，执行sql语句，如果执行完没有抛出移除就是正常的连接，反之
        if (result) {
            // 开启了侦测查询
            if (poolPingEnabled) {
                if (
                        poolPingConnectionsNotUsedFor >= 0 &&
                                conn.getTimeElapsedSinceLastUse() > poolPingConnectionsNotUsedFor
                ) {
                    try {
                        log.info("Testing connection " + conn.getRealHashCode() + " ...");
                        Connection realConn = conn.getRealConnection();
                        Statement statement = realConn.createStatement();
                        ResultSet resultSet = statement.executeQuery(poolPingQuery);
                        resultSet.close();
                        if (!realConn.getAutoCommit()) {
                            realConn.rollback();
                        }
                        result = true;
                        log.info("Connection " + conn.getRealHashCode() + " is GOOD!");
                    } catch (Exception e) {
                        log.info("Execution of ping query '" + poolPingQuery + "' failed: " + e.getMessage());
                        try {
                            conn.getRealConnection().close();
                        } catch (SQLException ignore) {
                        }
                        result = false;
                        log.info("Connection " + conn.getRealHashCode() + " is BAD: " + e.getMessage());
                    }
                }
            }

        }

        return result;
    }


    public static Connection unwrapConnection(Connection conn) {
        if (Proxy.isProxyClass(conn.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(conn);
            if (handler instanceof PooledConnection) {
                return ((PooledConnection) handler).getRealConnection();
            }
        }
        return conn;
    }

    private int assembleConnectionTypeCode(String url, String username, String password) {
        return ("" + url + username + password).hashCode();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return popConnection(dataSource.getUsername(), dataSource.getPassword()).getProxyConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return popConnection(username, password).getProxyConnection();
    }

    @Override
    protected void finalize() throws Throwable {
        forceCloseAll();
        super.finalize();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException(getClass().getName() + " is not a wrapper.");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter logWriter) throws SQLException {
        DriverManager.setLogWriter(logWriter);
    }

    @Override
    public void setLoginTimeout(int loginTimeout) throws SQLException {
        DriverManager.setLoginTimeout(loginTimeout);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

    public void setDriver(String driver) {
        dataSource.setDriver(driver);
        forceCloseAll();
    }

    public void setUrl(String url) {
        dataSource.setUrl(url);
        forceCloseAll();
    }

    public void setUsername(String username) {
        dataSource.setUsername(username);
        forceCloseAll();
    }

    public void setPassword(String password) {
        dataSource.setPassword(password);
        forceCloseAll();
    }


    public void setDefaultAutoCommit(boolean defaultAutoCommit) {
        dataSource.setAutoCommit(defaultAutoCommit);
        forceCloseAll();
    }

    public int getPoolMaximumActiveConnections() {
        return poolMaximumActiveConnections;
    }

    public void setPoolMaximumActiveConnections(int poolMaximumActiveConnections) {
        this.poolMaximumActiveConnections = poolMaximumActiveConnections;
    }

    public int getPoolMaximumIdleConnections() {
        return poolMaximumIdleConnections;
    }

    public void setPoolMaximumIdleConnections(int poolMaximumIdleConnections) {
        this.poolMaximumIdleConnections = poolMaximumIdleConnections;
    }

    public int getPoolMaximumCheckoutTime() {
        return poolMaximumCheckoutTime;
    }

    public void setPoolMaximumCheckoutTime(int poolMaximumCheckoutTime) {
        this.poolMaximumCheckoutTime = poolMaximumCheckoutTime;
    }

    public int getPoolTimeToWait() {
        return poolTimeToWait;
    }

    public void setPoolTimeToWait(int poolTimeToWait) {
        this.poolTimeToWait = poolTimeToWait;
    }

    public String getPoolPingQuery() {
        return poolPingQuery;
    }

    public void setPoolPingQuery(String poolPingQuery) {
        this.poolPingQuery = poolPingQuery;
    }

    public boolean isPoolPingEnabled() {
        return poolPingEnabled;
    }

    public void setPoolPingEnabled(boolean poolPingEnabled) {
        this.poolPingEnabled = poolPingEnabled;
    }

    public int getPoolPingConnectionsNotUsedFor() {
        return poolPingConnectionsNotUsedFor;
    }

    public void setPoolPingConnectionsNotUsedFor(int poolPingConnectionsNotUsedFor) {
        this.poolPingConnectionsNotUsedFor = poolPingConnectionsNotUsedFor;
    }

    public int getExpectedConnectionTypeCode() {
        return expectedConnectionTypeCode;
    }

    public void setExpectedConnectionTypeCode(int expectedConnectionTypeCode) {
        this.expectedConnectionTypeCode = expectedConnectionTypeCode;
    }
}
