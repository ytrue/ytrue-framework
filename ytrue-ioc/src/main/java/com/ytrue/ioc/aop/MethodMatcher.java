package com.ytrue.ioc.aop;

import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2022/10/14 08:40
 * @description 方法匹配，找到表达式范围内匹配下的目标类和方法
 */
public interface MethodMatcher {

    /**
     * Perform static checking whether the given method matches. If this
     *
     * @param method
     * @param targetClass
     * @return whether this method matches statically
     */
    boolean matches(Method method, Class<?> targetClass);
}
