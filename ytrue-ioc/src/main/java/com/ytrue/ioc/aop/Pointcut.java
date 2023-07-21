package com.ytrue.ioc.aop;

/**
 * @author ytrue
 * @date 2022/10/14 08:40
 * @description 切入点接口，定义用于获取 ClassFilter、MethodMatcher 的两个类，这两个接口获取都是切点表达式提供的内容
 */
public interface Pointcut {

    /**
     * Return the ClassFilter for this pointcut.
     *
     * @return the ClassFilter (never <code>null</code>)
     */
    ClassFilter getClassFilter();

    /**
     * Return the MethodMatcher for this pointcut.
     *
     * @return the MethodMatcher (never <code>null</code>)
     */
    MethodMatcher getMethodMatcher();
}
