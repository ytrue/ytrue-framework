package com.ytrue.netty.channel.socket;

import com.ytrue.netty.channel.ChannelConfig;

/**
 * @author ytrue
 * @date 2023-07-28 10:58
 * @description ServerSocketChannelConfig
 */
public interface ServerSocketChannelConfig extends ChannelConfig {

    /**
     * 返回ServerSocketChannel的等待连接队列的最大长度。等待连接队列是用于存放未处理的客户端连接请求的队列。
     *
     * @return
     */
    int getBacklog();

    /**
     * 设置ServerSocketChannel的等待连接队列的最大长度。
     *
     * @param backlog
     * @return
     */
    ServerSocketChannelConfig setBacklog(int backlog);

    /**
     * 返回ServerSocketChannel是否启用了地址重用。 。
     *
     * @return
     */
    boolean isReuseAddress();

    /**
     * 设置ServerSocketChannel是否启用地址重用。
     *
     * @param reuseAddress
     * @return
     */
    ServerSocketChannelConfig setReuseAddress(boolean reuseAddress);

    /**
     * 返回ServerSocketChannel的接收缓冲区大小。
     *
     * @return
     */
    int getReceiveBufferSize();

    /**
     * 设置ServerSocketChannel的接收缓冲区大小。
     *
     * @param receiveBufferSize
     * @return
     */
    ServerSocketChannelConfig setReceiveBufferSize(int receiveBufferSize);

    /**
     * setPerformancePreferences()是ServerSocketChannelConfig接口中的一个方法，用于设置性能偏好。
     * 性能偏好是指在处理网络连接时，优先考虑的某些方面，例如延迟、带宽或连接时间等。通过设置性能偏好，可以告诉操作系统在处理网络连接时优化特定的方面。
     * setPerformancePreferences()方法接受三个参数，分别是connectionTime、latency和bandwidth。这些参数的取值范围是0到100，其中0表示不重视该方面，100表示非常重视该方面。
     * - connectionTime表示连接时间的重视程度。较高的值表示优先考虑连接时间，即尽快建立连接。
     * - latency表示延迟的重视程度。较高的值表示优先考虑延迟，即尽量减少网络延迟。
     * - bandwidth表示带宽的重视程度。较高的值表示优先考虑带宽，即尽量提高数据传输速率。
     * 通过调用setPerformancePreferences()方法并传入适当的参数，可以告诉操作系统在处理网络连接时优化特定的方面，以满足应用程序的性能需求。
     * 但需要注意的是，具体的优化效果取决于操作系统的实现和网络环境的限制。
     *
     * @param connectionTime
     * @param latency
     * @param bandwidth
     * @return
     */
    ServerSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth);

    @Override
    ServerSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    ServerSocketChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    ServerSocketChannelConfig setAutoRead(boolean autoRead);

    @Override
    ServerSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark);

    @Override
    ServerSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark);
}
