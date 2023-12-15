package com.ytrue.web.resolver;

import com.ytrue.web.support.WebServletRequest;

import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2023-12-15 13:30
 * @description HandlerMethodReturnValueHandler
 */
public interface HandlerMethodReturnValueHandler {
    /**
     * 当前method 是否支持
     *
     * @param method
     * @return
     */
    boolean supportsReturnType(Method method);

    /**
     * 执行
     *
     * @param returnValue
     * @param webServletRequest
     * @throws Exception
     */
    void handleReturnValue(Object returnValue, WebServletRequest webServletRequest) throws Exception;
}
