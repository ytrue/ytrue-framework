package com.ytrue.web.support;

import com.ytrue.web.adapter.HandlerMethodAdapter;
import com.ytrue.web.adapter.RequestMappingHandlerMethodAdapter;
import com.ytrue.web.handler.HandlerMapping;
import com.ytrue.web.handler.RequestMappingHandlerMapping;
import com.ytrue.web.intercpetor.InterceptorRegistry;
import com.ytrue.web.intercpetor.MappedInterceptor;
import com.ytrue.web.resolver.DefaultHandlerExceptionResolver;
import com.ytrue.web.resolver.ExceptionHandlerExceptionResolver;
import com.ytrue.web.resolver.HandlerExceptionResolver;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-12-15 13:36
 * @description WebMvcConfigurationSupport
 */
public abstract class WebMvcConfigurationSupport {

    // 初始化组件

    @Bean
    public HandlerMapping handlerMapping() {

        final RequestMappingHandlerMapping requestMappingHandlerMapping = new RequestMappingHandlerMapping();
        requestMappingHandlerMapping.setOrder(0);
        final InterceptorRegistry registry = new InterceptorRegistry();
        getIntercept(registry);
        //  通过 registry 获取 MappedInterceptor
        // 获取拦截器
        final List<MappedInterceptor> interceptors = registry.getInterceptors();
        requestMappingHandlerMapping.addHandlerInterceptors(interceptors);
        // 添加拦截器
        return requestMappingHandlerMapping;
    }

    protected abstract void getIntercept(InterceptorRegistry registry);

    @Bean
    public HandlerMethodAdapter handlerMethodAdapter() {
        final RequestMappingHandlerMethodAdapter requestMappingHandlerMethodAdapter = new RequestMappingHandlerMethodAdapter();
        requestMappingHandlerMethodAdapter.setOrder(0);
        return requestMappingHandlerMethodAdapter;
    }

    @Bean
    public HandlerExceptionResolver defaultHandlerExceptionResolver() {
        final DefaultHandlerExceptionResolver defaultHandlerExceptionResolver = new DefaultHandlerExceptionResolver();
        defaultHandlerExceptionResolver.setOrder(1);
        return defaultHandlerExceptionResolver;
    }

    @Bean
    public HandlerExceptionResolver exceptionHandlerExceptionResolver() {
        final ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver = new ExceptionHandlerExceptionResolver();
        exceptionHandlerExceptionResolver.setOrder(0);
        return exceptionHandlerExceptionResolver;
    }

}

