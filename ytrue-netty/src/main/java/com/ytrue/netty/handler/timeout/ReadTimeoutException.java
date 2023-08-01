package com.ytrue.netty.handler.timeout;

/**
 * @author ytrue
 * @date 2023-08-01 8:59
 * @description ReadTimeoutException
 */
public class ReadTimeoutException extends TimeoutException {

    private static final long serialVersionUID = 169287984113283421L;

    public static final ReadTimeoutException INSTANCE = new ReadTimeoutException(true);

    ReadTimeoutException() { }

    private ReadTimeoutException(boolean shared) {
        super(shared);
    }
}
