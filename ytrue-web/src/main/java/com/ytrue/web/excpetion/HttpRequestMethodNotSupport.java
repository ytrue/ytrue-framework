package com.ytrue.web.excpetion;

/**
 * @author ytrue
 * @date 2023-12-15 10:38
 * @description HttpRequestMethodNotSupport
 */
public class HttpRequestMethodNotSupport extends Exception{
    public HttpRequestMethodNotSupport(String message) {
        super(message);
    }
}
