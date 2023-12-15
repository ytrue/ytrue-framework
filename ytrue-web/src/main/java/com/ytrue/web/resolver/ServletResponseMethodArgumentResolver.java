package com.ytrue.web.resolver;

import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.MethodParameter;

import javax.servlet.http.HttpServletResponse;

/**
 * @author ytrue
 * @date 2023-12-15 14:02
 * @description ServletResponseMethodArgumentResolver
 */
public class ServletResponseMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == HttpServletResponse.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HandlerMethod handlerMethod, WebServletRequest webServletRequest, ConvertComposite convertComposite) throws Exception {
        return webServletRequest.getResponse();
    }
}
