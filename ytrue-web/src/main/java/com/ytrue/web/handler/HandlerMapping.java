package com.ytrue.web.handler;

import org.springframework.core.Ordered;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ytrue
 * @date 2023-12-15 10:06
 * @description 映射器是用于根据请求路径找到对应的类/方法进行返回
 */
public interface HandlerMapping extends Ordered {


    /**
     * 根据请求路径获取对应的HandlerExecutionChain
     * @param request
     * @return
     * @throws Exception
     */
    HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
}
