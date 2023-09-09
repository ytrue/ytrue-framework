package com.ytrue.gateway.assist;

/**
 * @author ytrue
 * @date 2023-09-09 9:19
 * @description 网关异常
 */
public class GatewayException extends RuntimeException {

    public GatewayException(String msg) {
        super(msg);
    }

    public GatewayException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
