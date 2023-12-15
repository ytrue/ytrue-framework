package com.ytrue.web.resolver;

import com.ytrue.web.handler.HandlerMethod;
import org.springframework.core.Ordered;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ytrue
 * @date 2023-12-15 10:07
 * @description 异常解析器
 */
public interface HandlerExceptionResolver extends Ordered {

    Boolean resolveException(HttpServletRequest request, HttpServletResponse response, HandlerMethod handler, Exception ex) throws Exception;

    @Override
    int getOrder();
}
