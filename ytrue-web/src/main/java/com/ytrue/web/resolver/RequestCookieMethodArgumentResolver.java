package com.ytrue.web.resolver;

import com.ytrue.web.annotation.Cookie;
import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.excpetion.NotFoundException;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.MethodParameter;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ytrue
 * @date 2023-12-15 13:47
 * @description RequestCookieMethodArgumentResolver
 */
public class RequestCookieMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Cookie.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HandlerMethod handlerMethod, WebServletRequest webServletRequest, ConvertComposite convertComposite) throws Exception {

        final Cookie parameterAnnotation = parameter.getParameterAnnotation(Cookie.class);
        String name = "";
        name = parameterAnnotation.value().equals("") ? parameter.getParameterName() : parameterAnnotation.value();
        final HttpServletRequest request = webServletRequest.getRequest();
        // 获取所有cookie
        final javax.servlet.http.Cookie[] cookies = request.getCookies();
        // 遍历拿值
        for (javax.servlet.http.Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return convertComposite.convert(handlerMethod, parameter.getParameterType(), cookie.getValue());
            }
        }

        if (parameterAnnotation.require()) {
            throw new NotFoundException(handlerMethod.getPath() + "cookie没有携带: " + name);
        }

        return null;
    }
}
