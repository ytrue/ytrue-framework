package com.ytrue.web.handler;

import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2023-12-15 10:18
 * @description ExceptionHandlerMethod
 */
public class ExceptionHandlerMethod extends HandlerMethod{
    public ExceptionHandlerMethod(Object bean, Method method) {
        super(bean, method);
    }
}
