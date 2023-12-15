package com.ytrue.web.support;

import com.ytrue.web.intercpetor.InterceptorRegistry;

/**
 * @author ytrue
 * @date 2023-12-15 13:35
 * @description 定义拓展点规范供子类实现, 都是default
 */
public interface WebMvcConfigurer {


    /**
     * 添加拦截器
     *
     * @param registry
     */
    default void addIntercept(InterceptorRegistry registry) {
    }
}
