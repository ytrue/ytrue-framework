package com.ytrue.netty.channel;

import java.io.Serializable;

/**
 * @author ytrue
 * @date 2023-07-26 9:03
 * @description 在客户端连接建立后，生成Channel通道的时候会为每一个Channel分配一个唯一的ID
 */
public interface ChannelId  extends Serializable, Comparable<ChannelId> {

    String asShortText();


    String asLongText();
}
