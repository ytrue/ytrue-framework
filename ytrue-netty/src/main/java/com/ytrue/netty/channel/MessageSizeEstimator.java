package com.ytrue.netty.channel;

/**
 * @author ytrue
 * @date 2023-08-13 9:34
 * @description MessageSizeEstimator
 */
public interface MessageSizeEstimator {

    Handle newHandle();

    interface Handle {

        int size(Object msg);
    }
}
