package com.ytrue.netty.channel;

import com.ytrue.netty.util.IntSupplier;

/**
 * @author ytrue
 * @date 2023-07-24 9:46
 * @description 默认的策略选择
 */
public class DefaultSelectStrategy implements SelectStrategy {

    static final SelectStrategy INSTANCE = new DefaultSelectStrategy();

    private DefaultSelectStrategy() { }

    @Override
    public int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception {
        return hasTasks ? selectSupplier.get() : SelectStrategy.SELECT;
    }
}
