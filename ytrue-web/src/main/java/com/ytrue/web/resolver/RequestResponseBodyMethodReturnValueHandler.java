package com.ytrue.web.resolver;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ytrue.web.annotation.ResponseBody;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.annotation.AnnotatedElementUtils;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2023-12-15 13:40
 * @description RequestResponseBodyMethodReturnValueHandler
 */
public class RequestResponseBodyMethodReturnValueHandler implements HandlerMethodReturnValueHandler{

    final ObjectMapper objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    @Override
    public boolean supportsReturnType(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method.getDeclaringClass(), ResponseBody.class) || AnnotatedElementUtils.hasAnnotation(method,ResponseBody.class);
    }

    @Override
    public void handleReturnValue(Object returnValue, WebServletRequest webServletRequest) throws Exception {

        final HttpServletResponse response = webServletRequest.getResponse();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().println(objectMapper.writeValueAsString(returnValue));
        response.getWriter().flush();
    }
}
