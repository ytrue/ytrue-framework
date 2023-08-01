package com.ytrue.netty.handler.timeout;

/**
 * @author ytrue
 * @date 2023-08-01 9:00
 * @description WriteTimeoutException
 */
public class WriteTimeoutException extends TimeoutException {

    private static final long serialVersionUID = -144786655770296065L;

    public static final WriteTimeoutException INSTANCE = new WriteTimeoutException(true);

    private WriteTimeoutException() { }

    private WriteTimeoutException(boolean shared) {
        super(shared);
    }
}
