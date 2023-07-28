package com.ytrue.netty.channel.socket;

import com.ytrue.netty.channel.ChannelConfig;

/**
 * @author ytrue
 * @date 2023-07-28 10:58
 * @description SocketChannelConfig
 */
public interface SocketChannelConfig extends ChannelConfig {

    /**
     * 返回是否启用了TCP的Nagle算法。
     *
     * @return
     */
    boolean isTcpNoDelay();


    /**
     * 设置是否启用TCP的Nagle算法。如果启用，表示禁用Nagle算法，数据将立即发送；如果禁用，表示启用Nagle算法，数据将会缓冲一段时间后再发送。
     *
     * @param tcpNoDelay
     * @return
     */
    SocketChannelConfig setTcpNoDelay(boolean tcpNoDelay);


    /**
     * 返回SO_LINGER选项的值
     *
     * @return
     */
    int getSoLinger();


    /**
     * 设置SO_LINGER选项的值
     *
     * @param soLinger
     * @return
     */
    SocketChannelConfig setSoLinger(int soLinger);


    /**
     * 返回发送缓冲区的大小。
     *
     * @return
     */
    int getSendBufferSize();


    /**
     * 设置发送缓冲区的大小。
     *
     * @param sendBufferSize
     * @return
     */
    SocketChannelConfig setSendBufferSize(int sendBufferSize);


    /**
     * 设置接收缓冲区的大小。
     *
     * @return
     */
    int getReceiveBufferSize();

    /**
     * 接收缓冲区的大小。
     *
     * @param receiveBufferSize
     * @return
     */
    SocketChannelConfig setReceiveBufferSize(int receiveBufferSize);


    /**
     * 返回是否启用了TCP的KeepAlive机制。
     *
     * @return
     */
    boolean isKeepAlive();


    /**
     * 设置是否启用TCP的KeepAlive机制。如果启用，表示TCP连接空闲一段时间后会发送一个KeepAlive包以检测连接是否仍然有效。
     *
     * @param keepAlive
     * @return
     */
    SocketChannelConfig setKeepAlive(boolean keepAlive);


    /**
     * 返回IP头部的TOS或DSCP字段的值。
     *
     * @return
     */
    int getTrafficClass();


    /**
     * 设置IP头部的TOS或DSCP字段的值。该字段用于指定数据包的优先级和服务类型。
     *
     * @param trafficClass
     * @return
     */
    SocketChannelConfig setTrafficClass(int trafficClass);

    /**
     * 是否启用SO_REUSEADDR选项
     *
     * @return
     */
    boolean isReuseAddress();

    /**
     * 设置是否启用SO_REUSEADDR选项。如果启用，表示可以重用本地地址和端口，即使之前的连接还未完全关闭。
     *
     * @param reuseAddress
     * @return
     */
    SocketChannelConfig setReuseAddress(boolean reuseAddress);

    SocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth);


    /**
     * 方法用于返回是否允许半关闭的状态。半关闭是指在一个方向上关闭连接，即关闭输入流或输出流，而保持另一个方向上的连接仍然打开。如果返回true，则表示允许半关闭；如果返回false，则表示不允许半关闭。
     * 默认情况下，isAllowHalfClosure()方法返回false，即不允许半关闭。可以使用setAllowHalfClosure(boolean allowHalfClosure)方法来设置是否允许半关闭的状态。
     * 允许半关闭可以用于一些特定的应用场景，例如在一方发送完数据后关闭输出流，而另一方仍然可以继续接收数据。但需要注意的是，使用半关闭时需要确保双方都能正确处理半关闭的状态，
     * 否则可能会导致数据丢失或连接异常。因此，在使用半关闭时需要谨慎考虑，并确保双方都能正确处理。
     * Was the last answer useful?
     *
     * @return
     */
    boolean isAllowHalfClosure();

    /**
     * 设置版关闭状态
     *
     * @param allowHalfClosure
     * @return
     */
    SocketChannelConfig setAllowHalfClosure(boolean allowHalfClosure);

    @Override
    SocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    SocketChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    SocketChannelConfig setAutoRead(boolean autoRead);

    @Override
    SocketChannelConfig setAutoClose(boolean autoClose);
}
