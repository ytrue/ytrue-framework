package com.ytrue.web.resolver;

import com.ytrue.web.annotation.PathVariable;
import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.MethodParameter;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-12-15 13:44
 * @description PathVariableMethodArgumentResolver
 */
public class PathVariableMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(PathVariable.class) && parameter.getParameterType() != Map.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, HandlerMethod handlerMethod, WebServletRequest webServletRequest, ConvertComposite convertComposite) throws Exception {

        String name = "";
        Object result = null;
        // 1.获取 PathVariable 中的变量
        final PathVariable parameterAnnotation = parameter.getParameterAnnotation(PathVariable.class);
        name = parameterAnnotation.value().equals("") ?  parameter.getParameterName() : parameterAnnotation.value();

        Map<String,Object> resultMap = new HashMap<>();
        // 1.以/ 分割源path，找到变量 保存下标以及对应的变量
        final String path = handlerMethod.getPath();
        int index = -1;
        String[] split = path.split("/");
        for (int i = 0; i < split.length; i++) {
            final String s = split[i];
            if (s.contains("{") && s.contains("}") && s.contains(name)){
                index = i;
                break;
            }
        }
        final HttpServletRequest request = webServletRequest.getRequest();
        // 2.以/ 分割请求path，根据上一步找到的下标， 找到对应的值，放入resultMap
        split = request.getRequestURI().split("/");
        if (index != -1){
            result = split[index];
        }
        return convertComposite.convert(handlerMethod,parameter.getParameterType(),result);
    }

}
