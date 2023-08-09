package com.ytrue.netty.buffer;

/**
 * @author ytrue
 * @date 2023-08-09 14:28
 * @description ByteBufAllocatorMetricProvider
 */
public interface ByteBufAllocatorMetricProvider {


    ByteBufAllocatorMetric metric();
}
