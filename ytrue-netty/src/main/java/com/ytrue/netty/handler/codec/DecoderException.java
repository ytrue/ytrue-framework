package com.ytrue.netty.handler.codec;

/**
 * @author ytrue
 * @date 2023-08-14 14:24
 * @description DecoderException
 */

public class DecoderException extends CodecException {

    private static final long serialVersionUID = 6926716840699621852L;


    public DecoderException() {
    }


    public DecoderException(String message, Throwable cause) {
        super(message, cause);
    }


    public DecoderException(String message) {
        super(message);
    }


    public DecoderException(Throwable cause) {
        super(cause);
    }
}
