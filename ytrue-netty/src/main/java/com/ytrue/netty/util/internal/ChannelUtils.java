package com.ytrue.netty.util.internal;

/**
 * @author ytrue
 * @date 2023-08-13 14:23
 * @description ChannelUtils
 */
public final class ChannelUtils {
    public static final int MAX_BYTES_PER_GATHERING_WRITE_ATTEMPTED_LOW_THRESHOLD = 4096;
    public static final int WRITE_STATUS_SNDBUF_FULL = Integer.MAX_VALUE;

    private ChannelUtils() {
    }
}
