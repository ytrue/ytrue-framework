package com.ytrue.web.resolver;

import com.ytrue.web.annotation.RequestBody;
import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.multipart.MultipartFile;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.MethodParameter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-12-15 13:50
 * @description RequestParamMapMethodArgumentResolver
 */
public class RequestParamMapMethodArgumentResolver implements HandlerMethodArgumentResolver {


    /**
     * 写RequestParam 不写都可以, 会遇到其他的也有不写的场景，HttpServletRequest Mule...
     *
     * @param parameter
     * @return
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {

        final Class<?> parameterType = parameter.getParameterType();
        if (parameterType == HttpServletResponse.class || parameterType == HttpServletRequest.class) {
            return false;
        }
        if (isMultipartFile(parameter)) {
            return false;
        }
        if (parameter.hasParameterAnnotation(RequestBody.class)) {
            return false;
        }

        return parameterType == Map.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HandlerMethod handlerMethod, WebServletRequest webServletRequest, ConvertComposite convertComposite) throws Exception {

        Map<String, Object> resultMap = new HashMap<>();

        final HttpServletRequest request = webServletRequest.getRequest();
        final Map<String, String[]> parameterMap = request.getParameterMap();
        parameterMap.forEach((k, v) -> {
            resultMap.put(k, v[0]);
        });
        return resultMap;
    }


    /**
     * 是否是文件上传
     *
     * @param parameter
     * @return
     */
    public static boolean isMultipartFile(MethodParameter parameter) {

        final Class<?> parameterType = parameter.getParameterType();
        if (parameterType == MultipartFile.class) {
            return true;
        }

        if (parameterType == List.class || parameterType == Collection.class) {
            // 获取集合中的泛型是否是MultipartFile []
            final Type genericParameterType = parameter.getGenericParameterType();
            ParameterizedType type = (ParameterizedType) genericParameterType;

            if (type.getActualTypeArguments()[0] == MultipartFile.class) {
                return true;
            }
        }
        if (parameterType.isArray() && parameterType == MultipartFile.class) {
            return true;
        }

        return false;
    }
}
