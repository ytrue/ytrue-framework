package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-25 10:24
 * @description netty中的一个异常类，直接把源码复制过来了
 */
public class BlockingOperationException extends IllegalStateException {

    private static final long serialVersionUID = 2462223247762460301L;

    public BlockingOperationException() { }

    public BlockingOperationException(String s) {
        super(s);
    }

    public BlockingOperationException(Throwable cause) {
        super(cause);
    }

    public BlockingOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
