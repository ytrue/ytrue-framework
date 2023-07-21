package com.ytrue.orm.plugin;

import java.util.Properties;

/**
 * @author ytrue
 * @date 2022/9/6 16:32
 * @description 拦截器接口
 */
public interface Interceptor {

    /**
     * 拦截，使用方实现
     *
     * @param invocation
     * @return
     * @throws Throwable
     */
    Object intercept(Invocation invocation) throws Throwable;

    /**
     * 代理
     *
     * @param target
     * @return
     */
    default Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * 设置属性
     *
     * @param properties
     */
    default void setProperties(Properties properties) {
        // NOP
    }
}
