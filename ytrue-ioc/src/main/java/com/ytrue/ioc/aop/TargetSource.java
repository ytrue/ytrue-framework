package com.ytrue.ioc.aop;

import com.ytrue.ioc.util.ClassUtils;

/**
 * @author ytrue
 * @date 2022/10/14 08:56
 * @description TargetSource，是一个目标对象，在目标对象类中提供 Object 入参属性，以及获取目标类 TargetClass 信息
 */
public class TargetSource {

    private final Object target;

    public TargetSource(Object target) {
        this.target = target;
    }


    /**
     * 在TargetSourcetgetTargetClass用于获取target 对象的接口信息的，那么这个target 可能
     * 是JDK代理创建也可能是CGlib创建，为了保证都能正确的获取到结果,这里需要增加判读
     * ClassUtils. isCglibProxyClass(clazz)
     *
     * Return the type of targets returned by this {@link TargetSource}.
     * <p>Can return <code>null</code>, although certain usages of a
     * <code>TargetSource</code> might just work with a predetermined
     * target class.
     * @return the type of targets returned by this {@link TargetSource}
     */
    public Class<?>[] getTargetClass(){
        Class<?> clazz = this.target.getClass();
        clazz = ClassUtils.isCglibProxyClass(clazz) ? clazz.getSuperclass() : clazz;
        return clazz.getInterfaces();
    }

    /**
     * Return a target instance. Invoked immediately before the
     * AOP framework calls the "target" of an AOP method invocation.
     * @return the target object, which contains the joinpoint
     * @throws Exception if the target object can't be resolved
     */
    public Object getTarget(){
        return this.target;
    }
}
