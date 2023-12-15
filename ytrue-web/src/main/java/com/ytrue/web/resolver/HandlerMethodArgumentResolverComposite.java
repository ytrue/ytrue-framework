package com.ytrue.web.resolver;

import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.MethodParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-12-15 14:02
 * @description HandlerMethodArgumentResolverComposite
 */
public class HandlerMethodArgumentResolverComposite implements HandlerMethodArgumentResolver{

    final List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

    Map<MethodParameter,HandlerMethodArgumentResolver> argumentResolverCache = new HashMap<>();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        for (HandlerMethodArgumentResolver resolver : this.resolvers) {
            if (resolver.supportsParameter(parameter)) {
                argumentResolverCache.put(parameter,resolver);
                return true;
            }
        }
        // 返回false,说明我们没有参数解析器可以应对当前请求携带数据的场景
        return false;
    }

    protected HandlerMethodArgumentResolver getResolverArgument(MethodParameter parameter){
        return argumentResolverCache.get(parameter);
    }

    // 解析参数
    @Override
    public Object resolveArgument(MethodParameter parameter, HandlerMethod handlerMethod, WebServletRequest webServletRequest, ConvertComposite convertComposite) throws Exception {
        // 1.获取对应的参数解析器
        final HandlerMethodArgumentResolver resolverArgument = getResolverArgument(parameter);
        return resolverArgument.resolveArgument(parameter,handlerMethod,webServletRequest,convertComposite);
    }

    public void addResolvers(List<HandlerMethodArgumentResolver> resolvers){
        this.resolvers.addAll(resolvers);
    }
}
