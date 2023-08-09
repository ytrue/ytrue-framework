package com.ytrue.netty.buffer;

/**
 * @author ytrue
 * @date 2023-08-06 15:22
 * @description PoolSubpageMetric
 */
public interface PoolSubpageMetric {


    int maxNumElements();


    int numAvailable();


    int elementSize();


    int pageSize();
}
