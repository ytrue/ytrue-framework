package com.ytrue.ioc.aop;


import org.aopalliance.aop.Advice;

/**
 * @author ytrue
 * @date 2022/10/14 09:55
 * @description Advisor
 */
public interface Advisor {

    /**
     * 获取通知
     * Return the advice part of this aspect. An advice may be an
     * interceptor, a before advice, a throws advice, etc.
     *
     * @return the advice that should apply if the pointcut matches
     * @see org.aopalliance.intercept.MethodInterceptor
     * @see BeforeAdvice
     */
    Advice getAdvice();
}
