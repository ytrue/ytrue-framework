package com.ytrue.web.intercpetor;

import com.ytrue.web.handler.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ytrue
 * @date 2023-12-15 10:19
 * @description 拦截器
 */
public interface HandlerInterceptor {

    /**
     * 预处理
     *
     * @param request
     * @param response
     * @return
     */
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        return true;
    }

    /**
     * 执行完控制器处理
     *
     * @param request
     * @param response
     */
    default void postHandle(HttpServletRequest request, HttpServletResponse response) {
    }


    /**
     * 渲染视图后处理
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     */
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, HandlerMethod handler,
                                 Exception ex) {
    }
}
