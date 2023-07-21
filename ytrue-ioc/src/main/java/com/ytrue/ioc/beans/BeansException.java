package com.ytrue.ioc.beans;

/**
 * @author ytrue
 * @date 2022/9/28 09:14
 * @description Bean异常
 */
public class BeansException extends RuntimeException {

    public BeansException(String msg) {
        super(msg);
    }

    public BeansException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
