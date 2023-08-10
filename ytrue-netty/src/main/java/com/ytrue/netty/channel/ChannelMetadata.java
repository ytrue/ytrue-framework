package com.ytrue.netty.channel;

import static com.ytrue.netty.util.internal.ObjectUtil.checkPositive;

/**
 * @author ytrue
 * @date 2023-08-10 9:47
 * @description ChannelMetadata
 */
public final class ChannelMetadata {

    private final boolean hasDisconnect;
    //这个值为16，创建ChannelMetadata对象的时候会发现该值被设置成16了
    private final int defaultMaxMessagesPerRead;

    public ChannelMetadata(boolean hasDisconnect) {
        this(hasDisconnect, 1);
    }


    public ChannelMetadata(boolean hasDisconnect, int defaultMaxMessagesPerRead) {
        checkPositive(defaultMaxMessagesPerRead, "defaultMaxMessagesPerRead");

        this.hasDisconnect = hasDisconnect;
        this.defaultMaxMessagesPerRead = defaultMaxMessagesPerRead;
    }


    public boolean hasDisconnect() {
        return hasDisconnect;
    }


    public int defaultMaxMessagesPerRead() {
        return defaultMaxMessagesPerRead;
    }
}
