package com.ytrue.ioc.aop.framework;

import com.ytrue.ioc.aop.AdvisedSupport;
import org.aopalliance.intercept.MethodInterceptor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author ytrue
 * @date 2022/10/14 09:01
 * @description 基于 JDK 实现的代理类，需要实现接口 AopProxy、InvocationHandler，这样就可以把代理对象 getProxy 和反射调用方法 invoke 分开处理了。
 * getProxy 方法中的是代理一个对象的操作，需要提供入参 ClassLoader、AdvisedSupport、和当前这个类 this，因为这个类提供了 invoke 方法。
 * invoke 方法中主要处理匹配的方法后，使用用户自己提供的方法拦截实现，做反射调用 methodInterceptor.invoke 。
 * 这里还有一个 ReflectiveMethodInvocation，其他它就是一个入参的包装信息，提供了入参对象：目标对象、方法、入参。
 */
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {

    private final AdvisedSupport advised;

    public JdkDynamicAopProxy(AdvisedSupport advised) {
        this.advised = advised;
    }

    @Override
    public Object getProxy() {
        // 获取jdk代理对象
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), advised.getTargetSource().getTargetClass(), this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 如果方法匹配
        if (advised.getMethodMatcher().matches(method, advised.getTargetSource().getTarget().getClass())) {
            // 获取具体拦截方法实现类
            MethodInterceptor methodInterceptor = advised.getMethodInterceptor();
            return methodInterceptor.invoke(new ReflectiveMethodInvocation(advised.getTargetSource().getTarget(), method, args));
        }
        // 不匹配就执行原来方法
        return method.invoke(advised.getTargetSource().getTarget(), args);
    }
}
