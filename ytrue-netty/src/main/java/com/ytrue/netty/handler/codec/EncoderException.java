package com.ytrue.netty.handler.codec;

/**
 * @author ytrue
 * @date 2023-08-14 14:24
 * @description EncoderException
 */
public class EncoderException extends CodecException {

    private static final long serialVersionUID = -5086121160476476774L;


    public EncoderException() {
    }


    public EncoderException(String message, Throwable cause) {
        super(message, cause);
    }


    public EncoderException(String message) {
        super(message);
    }


    public EncoderException(Throwable cause) {
        super(cause);
    }
}
