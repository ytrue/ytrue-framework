package com.ytrue.netty.channel;

/**
 * @author ytrue
 * @date 2023-08-10 9:39
 * @description MaxMessagesRecvByteBufAllocator
 */
public interface MaxMessagesRecvByteBufAllocator extends RecvByteBufAllocator {

    int maxMessagesPerRead();


    MaxMessagesRecvByteBufAllocator maxMessagesPerRead(int maxMessagesPerRead);
}
