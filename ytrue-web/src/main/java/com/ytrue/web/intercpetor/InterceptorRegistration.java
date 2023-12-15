package com.ytrue.web.intercpetor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-12-15 10:28
 * @description 封装拦截器信息
 */
public class InterceptorRegistration {


    /**
     * 拦截器
     */
    private HandlerInterceptor interceptor;
    /**
     * 拦截路径
     */
    private List<String> includePatterns = new ArrayList<>();
    /**
     * 排除路径
     */
    private List<String> excludePatterns = new ArrayList<>();



    public HandlerInterceptor getInterceptor() {
        return interceptor;
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public List<String> getIncludePatterns() {
        return includePatterns;
    }

    public InterceptorRegistration addExcludePatterns(String... path) {
        this.excludePatterns.addAll(Arrays.asList(path));
        return this;
    }


    public InterceptorRegistration addIncludePatterns(String... path) {
        this.includePatterns.addAll(Arrays.asList(path));
        return this;
    }

    public InterceptorRegistration setInterceptor(HandlerInterceptor interceptor) {
        this.interceptor = interceptor;
        return this;
    }

}
