package com.ytrue.web.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ytrue.web.annotation.RequestBody;
import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.MethodParameter;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;

/**
 * @author ytrue
 * @date 2023-12-15 14:00
 * @description RequestRequestBodyMethodArgumentResolver
 */
public class RequestRequestBodyMethodArgumentResolver implements HandlerMethodArgumentResolver {

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestBody.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HandlerMethod handlerMethod, WebServletRequest webServletRequest, ConvertComposite convertComposite) throws Exception {

        final String json = getJson(webServletRequest.getRequest());
        return objectMapper.readValue(json, parameter.getParameterType());
    }

    public String getJson(HttpServletRequest request){

        final StringBuilder builder = new StringBuilder();
        String line = null;
        try(final BufferedReader reader = request.getReader()) {
            while(line != (line = reader.readLine())){
                builder.append(line);
            }
        }catch (Exception e){
            e.printStackTrace();
        }


        return builder.toString();
    }
}
