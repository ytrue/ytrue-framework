package com.ytrue.web.handler;

import com.ytrue.web.intercpetor.HandlerInterceptor;
import com.ytrue.web.intercpetor.MappedInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-12-15 10:12
 * @description HandlerExecutionChain 执行链
 */
public class HandlerExecutionChain {

    /**
     * 处理的方法
     */
    private final HandlerMethod handlerMethod;

    /**
     * 拦截器
     */
    private List<HandlerInterceptor> interceptors = new ArrayList<>();


    public HandlerExecutionChain(HandlerMethod handlerMethod) {
        this.handlerMethod = handlerMethod;
    }

    public HandlerMethod getHandlerMethod() {
        return handlerMethod;
    }


    /**
     * 设置合适的拦截器
     *
     * @param interceptors
     */
    public void setInterceptors(List<HandlerInterceptor> interceptors) {
        // 路径映射匹配
        for (HandlerInterceptor interceptor : interceptors) {
            if (interceptor instanceof MappedInterceptor) {
                // 是否匹配这个路径
                if (((MappedInterceptor) interceptor).match(handlerMethod.getPath())) {
                    // 加入进去
                    this.interceptors.add(interceptor);
                }
            } else {
                this.interceptors.add(interceptor);
            }
        }
    }


    /**
     * 多个拦截器执行，一旦有一个拦截器返回false，整个链路可以崩掉
     *
     * @param req
     * @param resp
     * @return
     */
    public boolean applyPreInterceptor(HttpServletRequest req, HttpServletResponse resp) {
        for (HandlerInterceptor interceptor : this.interceptors) {
            if (!interceptor.preHandle(req, resp)) {
                return false;
            }
        }
        return true;
    }


    /**
     * 执行postHandle
     *
     * @param req
     * @param resp
     */
    public void applyPostInterceptor(HttpServletRequest req, HttpServletResponse resp) {

        for (HandlerInterceptor interceptor : this.interceptors) {
            interceptor.postHandle(req, resp);
        }
    }

    /**
     * 执行afterCompletion
     *
     * @param req
     * @param resp
     * @param handlerMethod
     * @param ex
     */
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, HandlerMethod handlerMethod, Exception ex) {
        for (HandlerInterceptor interceptor : this.interceptors) {
            interceptor.afterCompletion(req, resp, handlerMethod, ex);
        }
    }
}
