package com.ytrue.web.intercpetor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ytrue
 * @date 2023-12-15 10:30
 * @description 拦截器注册
 */
public class InterceptorRegistry {

    /**
     * 拦截器集合
     */
    private List<InterceptorRegistration> interceptorRegistrations = new ArrayList<>();

    /**
     * 添加拦截器
     *
     * @param interceptor
     * @return
     */
    public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
        final InterceptorRegistration interceptorRegistration = new InterceptorRegistration();
        // 设置拦截器
        interceptorRegistration.setInterceptor(interceptor);
        // 加入进去
        interceptorRegistrations.add(interceptorRegistration);
        return interceptorRegistration;
    }


    /**
     * 转换成路径映射匹配的拦截器
     *
     * @return
     */
    public List<MappedInterceptor> getInterceptors() {
        final List<MappedInterceptor> mappedInterceptorList = this.interceptorRegistrations.stream().map(MappedInterceptor::new).collect(Collectors.toList());
        return mappedInterceptorList;
    }
}
