package com.ytrue.netty.channel.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * @author ytrue
 * @date 2023-08-14 15:48
 * @description NioTask
 */
public interface NioTask<C extends SelectableChannel> {

    void channelReady(C ch, SelectionKey key) throws Exception;


    void channelUnregistered(C ch, Throwable cause) throws Exception;
}
