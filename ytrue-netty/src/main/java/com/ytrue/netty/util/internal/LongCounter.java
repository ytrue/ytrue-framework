package com.ytrue.netty.util.internal;

/**
 * @author ytrue
 * @date 2023-08-07 11:48
 * @description LongCounter
 */
public interface LongCounter {
    void add(long delta);
    void increment();
    void decrement();
    long value();
}
