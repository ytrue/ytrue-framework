package com.ytrue.web.adapter;

import com.ytrue.web.handler.HandlerMethod;
import org.springframework.core.Ordered;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ytrue
 * @date 2023-12-15 10:06
 * @description 适配器接口
 */
public interface HandlerMethodAdapter extends Ordered {

    boolean support(HandlerMethod handlerMethod);

    void handler(HttpServletRequest req, HttpServletResponse res, HandlerMethod handler) throws Exception;


}
