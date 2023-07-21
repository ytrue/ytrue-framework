package com.ytrue.ioc.aop;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * @author ytrue
 * @date 2022/10/14 08:56
 * @description AdvisedSupport，主要是用于把代理、拦截、匹配的各项属性包装到一个类中，方便在 Proxy 实现类进行使用
 */
public class AdvisedSupport {

    /**
     * 是否使用cglib
     */
    private boolean proxyTargetClass = false;

    /**
     * 被代理的目标对象
     */
    private TargetSource targetSource;

    /**
     * MethodInterceptor，是一个具体拦截方法实现类，由用户自己实现 MethodInterceptor#invoke 方法，做具体的处理
     */
    private MethodInterceptor methodInterceptor;

    /**
     * 是一个匹配方法的操作，这个对象由 AspectJExpressionPointcut 提供服务
     * 方法匹配器(检查目标方法是否符合通知条件)
     */
    private MethodMatcher methodMatcher;


    public TargetSource getTargetSource() {
        return targetSource;
    }

    public void setTargetSource(TargetSource targetSource) {
        this.targetSource = targetSource;
    }

    public MethodInterceptor getMethodInterceptor() {
        return methodInterceptor;
    }

    public void setMethodInterceptor(MethodInterceptor methodInterceptor) {
        this.methodInterceptor = methodInterceptor;
    }

    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    public void setMethodMatcher(MethodMatcher methodMatcher) {
        this.methodMatcher = methodMatcher;
    }

    public boolean isProxyTargetClass() {
        return proxyTargetClass;
    }

    public void setProxyTargetClass(boolean proxyTargetClass) {
        this.proxyTargetClass = proxyTargetClass;
    }
}
