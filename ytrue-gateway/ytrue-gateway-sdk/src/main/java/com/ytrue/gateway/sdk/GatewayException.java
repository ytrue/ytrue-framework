package com.ytrue.gateway.sdk;

/**
 * @author ytrue
 * @date 2023-09-09 9:53
 * @description GatewayException
 */
public class GatewayException extends RuntimeException {

    public GatewayException(String msg) {
        super(msg);
    }

    public GatewayException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
