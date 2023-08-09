package com.ytrue.netty.buffer;

/**
 * @author ytrue
 * @date 2023-08-09 14:29
 * @description 决定使用直接内存还是堆内存
 */
public interface ByteBufAllocatorMetric {

    long usedHeapMemory();


    long usedDirectMemory();
}
