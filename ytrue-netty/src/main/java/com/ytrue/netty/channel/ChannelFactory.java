package com.ytrue.netty.channel;


/**
 * @author ytrue
 * @date 2023-07-26 9:06
 * @description ChannelFactory
 */
public interface ChannelFactory<T extends Channel> {

    T newChannel();
}
