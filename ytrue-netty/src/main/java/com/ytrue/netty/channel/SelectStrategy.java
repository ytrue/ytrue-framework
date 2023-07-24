package com.ytrue.netty.channel;

import com.ytrue.netty.util.IntSupplier;

/**
 * @author ytrue
 * @date 2023-07-24 9:46
 * @description 选择策略
 */
public interface SelectStrategy {

    int SELECT = -1;

    int CONTINUE = -2;

    int BUSY_WAIT = -3;

    /**
     * 计算策略
     *
     * @param selectSupplier
     * @param hasTasks
     * @return
     * @throws Exception
     */
    int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception;
}
