package com.ytrue.ioc.aop;

/**
 * @author ytrue
 * @date 2022/10/14 08:40
 * @description 定义类匹配类，用于切点找到给定的接口和目标类
 */
public interface ClassFilter {

    /**
     * Should the pointcut apply to the given interface or target class?
     * @param clazz the candidate target class
     * @return whether the advice should apply to the given target class
     */
    boolean matches(Class<?> clazz);
}
