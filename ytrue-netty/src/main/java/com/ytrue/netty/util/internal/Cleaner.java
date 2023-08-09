package com.ytrue.netty.util.internal;

import java.nio.ByteBuffer;

/**
 * @author ytrue
 * @date 2023-08-07 11:47
 * @description Cleaner
 */
public interface Cleaner {

    /**
     * 释放直接内存
     *
     * @param buffer
     */
    void freeDirectBuffer(ByteBuffer buffer);
}
