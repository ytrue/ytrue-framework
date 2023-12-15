package com.ytrue.web.resolver;

import com.ytrue.web.excpetion.ConvertCastException;
import com.ytrue.web.excpetion.HttpRequestMethodNotSupport;
import com.ytrue.web.excpetion.NotFoundException;
import com.ytrue.web.handler.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ytrue
 * @date 2023-12-15 13:36
 * @description 默认异常解析器，尽可能的枚举所有上层发生的异常进行处理
 */
public class DefaultHandlerExceptionResolver implements HandlerExceptionResolver {

    private int order;

    @Override
    public Boolean resolveException(HttpServletRequest request, HttpServletResponse response, HandlerMethod handler, Exception ex) throws Exception {
        final Class<? extends Exception> type = ex.getClass();
        if (type == ConvertCastException.class) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            return true;
        } else if (type == HttpRequestMethodNotSupport.class) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
            return true;
        } else if (type == NotFoundException.class) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            return true;
        }
        return false;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
