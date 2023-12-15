package com.ytrue.web.resolver;

import com.ytrue.web.annotation.RequestHeader;
import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.MethodParameter;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-12-15 13:48
 * @description RequestHeaderMapMethodArgumentResolver
 */
public class RequestHeaderMapMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {

        return parameter.hasParameterAnnotation(RequestHeader.class) && parameter.getParameterType() == Map.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HandlerMethod handlerMethod, WebServletRequest webServletRequest, ConvertComposite convertComposite) throws Exception {

        final HttpServletRequest request = webServletRequest.getRequest();
        final Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, String> resultMap = new HashMap<>();
        while (headerNames.hasMoreElements()) {
            final String key = headerNames.nextElement();
            final String value = request.getHeader(key);
            resultMap.put(key, value);
        }

        return resultMap;
    }
}
