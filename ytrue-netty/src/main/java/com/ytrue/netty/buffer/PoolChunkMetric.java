package com.ytrue.netty.buffer;

/**
 * @author ytrue
 * @date 2023-08-06 11:46
 * @description 提供关于Netty中池块（Pool Chunk）的度量指标信息。
 * 它允许用户获取关于池块的使用情况、内存大小、空闲字节数等相关信息。通过这些指标，
 * 可以监控和评估池块在内存管理中的性能和效率。这些指标对于优化内存分配和资源利用非常有用。
 */
public interface PoolChunkMetric {


    /**
     * 返回块使用的内存百分比。
     *
     * @return
     */
    int usage();


    /**
     * 返回块的大小（以字节为单位）。
     *
     * @return
     */
    int chunkSize();


    /**
     * 返回块中的空闲字节数。
     *
     * @return
     */
    int freeBytes();
}
