package com.ytrue.ioc.aop.framework;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2022/10/14 09:06
 *
 * 使用反射调用目标对象。子类可以覆盖#invokeJoinpoint（）方法来更改此行为，因此对于更专业的MethodInvocation实现来说，这也是一个有用的基类。
 * <p>Invokes the target object using reflection. Subclasses can override the
 * #invokeJoinpoint() method to change this behavior, so this is also
 * a useful base class for more specialized MethodInvocation implementations.
 * <p>
 */
public class ReflectiveMethodInvocation implements MethodInvocation {


    /**
     * 目标对象
     */
    protected final Object target;

    /**
     * 方法
     */
    protected final Method method;

    /**
     * 入参
     */
    protected final Object[] arguments;

    public ReflectiveMethodInvocation(Object target, Method method, Object[] arguments) {
        this.target = target;
        this.method = method;
        this.arguments = arguments;
    }

    @Override
    public Method getMethod() {
        return this.method;
    }

    @Override
    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public Object proceed() throws Throwable {
        return method.invoke(target, arguments);
    }

    @Override
    public Object getThis() {
        return target;
    }

    @Override
    public AccessibleObject getStaticPart() {
        return method;
    }
}
