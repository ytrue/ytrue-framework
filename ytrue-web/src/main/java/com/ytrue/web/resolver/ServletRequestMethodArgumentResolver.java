package com.ytrue.web.resolver;

import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.MethodParameter;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ytrue
 * @date 2023-12-15 14:01
 * @description ServletRequestMethodArgumentResolver
 */
public class ServletRequestMethodArgumentResolver implements HandlerMethodArgumentResolver{
    @Override
    public boolean supportsParameter(MethodParameter parameter) {

        return parameter.getParameterType() == HttpServletRequest.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HandlerMethod handlerMethod, WebServletRequest webServletRequest, ConvertComposite convertComposite) throws Exception {
        return webServletRequest.getRequest();
    }
}
