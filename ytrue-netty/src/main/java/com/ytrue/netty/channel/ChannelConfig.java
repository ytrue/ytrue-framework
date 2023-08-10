package com.ytrue.netty.channel;

import com.ytrue.netty.buffer.ByteBufAllocator;

import java.util.Map;

/**
 * @author ytrue
 * @date 2023-07-26 9:10
 * @description 暂时不定义方法，后面在处理
 */
public interface ChannelConfig {


    /**
     * 该方法是返回存储所有常量类和常量类对应的值的map
     *
     * @return
     */
    Map<ChannelOption<?>, Object> getOptions();

    /**
     * 批量设置参数
     *
     * @param options
     * @return
     */
    boolean setOptions(Map<ChannelOption<?>, ?> options);

    /**
     * 获取参数
     *
     * @param option
     * @param <T>
     * @return
     */
    <T> T getOption(ChannelOption<T> option);

    /**
     * 设置参数
     *
     * @param option
     * @param value
     * @param <T>
     * @return
     */
    <T> boolean setOption(ChannelOption<T> option, T value);


    /**
     * 获取连接超时的时间
     *
     * @return
     */
    int getConnectTimeoutMillis();

    /**
     * 设置连接超时时间
     *
     * @param connectTimeoutMillis
     * @return
     */
    ChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);


    /**
     * 获取写操作的自旋次数，这个水位线暂时还用不到，等后面重构发送数据方法时，我们会真正用到的
     *
     * @return
     */
    int getWriteSpinCount();

    /**
     * 设置写操作的自旋次数。
     *
     * @param writeSpinCount
     * @return
     */
    ChannelConfig setWriteSpinCount(int writeSpinCount);


    /**
     * 是否自动读
     *
     * @return
     */
    boolean isAutoRead();

    /**
     * 设置是否自动读
     *
     * @param autoRead
     * @return
     */
    ChannelConfig setAutoRead(boolean autoRead);

    /**
     * 是否关闭
     *
     * @return
     */
    boolean isAutoClose();

    /**
     * 设置是否关闭
     *
     * @param autoClose
     * @return
     */
    ChannelConfig setAutoClose(boolean autoClose);


    /**
     * 获取用于估计消息大小的MessageSizeEstimator。
     */
    int getWriteBufferLowWaterMark();

    /**
     * 设置写缓冲区的低水位标记。
     *
     * @param writeBufferLowWaterMark
     * @return
     */
    ChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark);

    /**
     * 设置写缓冲区的高水位标记
     *
     * @param writeBufferHighWaterMark
     * @return
     */
    ChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark);


    /**
     * 动态内存分配器终于也添加进来了
     * @return
     * @param <T>
     */
    <T extends RecvByteBufAllocator> T getRecvByteBufAllocator();

    ChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator);


    /**
     * 内存分配器终于添加进来了
     * @return
     */
    ByteBufAllocator getAllocator();

    ChannelConfig setAllocator(ByteBufAllocator allocator);
}
