package com.ytrue.netty.handler.timeout;

import com.ytrue.netty.channel.ChannelException;

/**
 * @author ytrue
 * @date 2023-08-01 9:00
 * @description TimeoutException
 */
public class TimeoutException extends ChannelException {

    private static final long serialVersionUID = 4673641882869672533L;

    TimeoutException() {
    }

    TimeoutException(boolean shared) {
        super(null, null, shared);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
