package com.ytrue.netty.channel;

/**
 * @author ytrue
 * @date 2023-07-24 9:47
 * @description SelectStrategyFactory
 */
public interface SelectStrategyFactory {

    SelectStrategy newSelectStrategy();
}
