package com.ytrue.netty.handler.codec;

/**
 * @author ytrue
 * @date 2023-08-14 14:24
 * @description CodecException
 */
public class CodecException extends RuntimeException {

    private static final long serialVersionUID = -1464830400709348473L;


    public CodecException() {
    }


    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }


    public CodecException(String message) {
        super(message);
    }


    public CodecException(Throwable cause) {
        super(cause);
    }
}
