package com.ytrue.netty.channel;

/**
 * @author ytrue
 * @date 2023-07-24 9:48
 * @description DefaultSelectStrategyFactory
 */
public class DefaultSelectStrategyFactory implements SelectStrategyFactory {

    private DefaultSelectStrategyFactory() {
    }

    public static final SelectStrategyFactory INSTANCE = new DefaultSelectStrategyFactory();

    @Override
    public SelectStrategy newSelectStrategy() {
        return DefaultSelectStrategy.INSTANCE;
    }
}
