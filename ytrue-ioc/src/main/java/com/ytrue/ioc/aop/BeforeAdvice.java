package com.ytrue.ioc.aop;

import org.aopalliance.aop.Advice;

/**
 * @author ytrue
 * @date 2022/10/14 09:49
 * @description 在 Spring 框架中，Advice 都是通过方法拦截器 MethodInterceptor 实现的。
 * 环绕 Advice 类似一个拦截器的链路，Before Advice、After advice等，
 * 不过暂时我们需要那么多就只定义了一个 MethodBeforeAdvice 的接口定义。
 */
public interface BeforeAdvice extends Advice {
}
