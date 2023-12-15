package com.ytrue.web.resolver;

import com.ytrue.web.annotation.RequestHeader;
import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.excpetion.NotFoundException;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-12-15 13:49
 * @description RequestHeaderMethodArgumentResolver
 */
public class RequestHeaderMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestHeader.class) && parameter.getParameterType() != Map.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HandlerMethod handlerMethod, WebServletRequest webServletRequest, ConvertComposite convertComposite) throws Exception {

        String name = "";
        final RequestHeader parameterAnnotation = parameter.getParameterAnnotation(RequestHeader.class);
        name = parameterAnnotation.value().equals("") ? parameter.getParameterName() : parameterAnnotation.value();

        final HttpServletRequest request = webServletRequest.getRequest();
        if (parameterAnnotation.require() && ObjectUtils.isEmpty(request.getHeader(name))) {
            throw new NotFoundException(handlerMethod.getPath() + "请求头没有携带: " + name);
        }
        return convertComposite.convert(handlerMethod, parameter.getParameterType(), request.getHeader(name));

    }
}
