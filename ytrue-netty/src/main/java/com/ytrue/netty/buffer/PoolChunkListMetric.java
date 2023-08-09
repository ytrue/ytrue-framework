package com.ytrue.netty.buffer;

/**
 * @author ytrue
 * @date 2023-08-08 9:15
 * @description PoolChunkListMetric
 */
public interface PoolChunkListMetric extends Iterable<PoolChunkMetric>{

    int minUsage();


    int maxUsage();
}
