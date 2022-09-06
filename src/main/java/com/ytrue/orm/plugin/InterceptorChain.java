package com.ytrue.orm.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/9/6 16:37
 * @description 拦截器链
 */
public class InterceptorChain {
    /**
     * 拦截器集合
     */
    private final List<Interceptor> interceptors = new ArrayList<>();

    /**
     * 执行
     *
     * @param target
     * @return
     */
    public Object pluginAll(Object target) {
        for (Interceptor interceptor : interceptors) {
            target = interceptor.plugin(target);
        }
        return target;
    }

    /**
     * 添加拦截器
     *
     * @param interceptor
     */
    public void addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
    }

    /**
     * 获取所有的拦截器
     *
     * @return
     */
    public List<Interceptor> getInterceptors() {
        return Collections.unmodifiableList(interceptors);
    }
}
