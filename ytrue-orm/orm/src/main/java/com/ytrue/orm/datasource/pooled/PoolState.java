package com.ytrue.orm.datasource.pooled;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/17 14:44
 * @description 池状态
 */
public class PoolState {

    protected PooledDataSource dataSource;

    /**
     * 空闲链接
     */
    protected final List<PooledConnection> idleConnections = new ArrayList<>();

    /**
     * 活跃链接
     */
    protected final List<PooledConnection> activeConnections = new ArrayList<>();

    /**
     * 请求次数
     */
    protected long requestCount = 0;

    /**
     * 总请求时间
     */
    protected long accumulatedRequestTime = 0;

    /**
     * 累计Checkout时间
     */
    protected long accumulatedCheckoutTime = 0;

    /**
     * 检查时间过长的连接数量，就是过期的连接数量
     */
    protected long claimedOverdueConnectionCount = 0;

    /**
     * 累计 连接过期的时间
     */
    protected long accumulatedCheckoutTimeOfOverdueConnections = 0;

    /**
     * 总等待时间
     */
    protected long accumulatedWaitTime = 0;

    /**
     * 要等待的次数
     */
    protected long hadToWaitCount = 0;

    /**
     * 失败连接次数
     */
    protected long badConnectionCount = 0;


    public PoolState(PooledDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 获取请求次数
     *
     * @return
     */
    public synchronized long getRequestCount() {
        return requestCount;
    }

    /**
     * 获取平均的请求时间
     *
     * @return
     */
    public synchronized long getAverageRequestTime() {
        return requestCount == 0 ? 0 : accumulatedRequestTime / requestCount;
    }

    /**
     * 获取平均的等待时间
     *
     * @return
     */
    public synchronized long getAverageWaitTime() {
        return hadToWaitCount == 0 ? 0 : accumulatedWaitTime / hadToWaitCount;
    }

    /**
     * 获取要等待的次数
     *
     * @return
     */
    public synchronized long getHadToWaitCount() {
        return hadToWaitCount;
    }

    /**
     * 获取失败连接次数
     *
     * @return
     */
    public synchronized long getBadConnectionCount() {
        return badConnectionCount;
    }

    /**
     * 获取声明的过期连接计数
     *
     * @return
     */
    public synchronized long getClaimedOverdueConnectionCount() {
        return claimedOverdueConnectionCount;
    }

    /**
     * 获取平均Checkout时间
     *
     * @return
     */
    public synchronized long getAverageOverdueCheckoutTime() {
        return claimedOverdueConnectionCount == 0 ? 0 : accumulatedCheckoutTimeOfOverdueConnections / claimedOverdueConnectionCount;
    }


    /**
     * 获取平均结帐时间
     *
     * @return
     */
    public synchronized long getAverageCheckoutTime() {
        return requestCount == 0 ? 0 : accumulatedCheckoutTime / requestCount;
    }

    /**
     * 获取空闲的连接数量
     *
     * @return
     */
    public synchronized int getIdleConnectionCount() {
        return idleConnections.size();
    }

    /**
     * 获取活跃的连接数量
     *
     * @return
     */
    public synchronized int getActiveConnectionCount() {
        return activeConnections.size();
    }
}
