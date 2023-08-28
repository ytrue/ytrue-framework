package com.ytrue.job.core.handler.impl;

import com.ytrue.job.core.handler.IJobHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2023-08-28 11:34
 * @description 该类的作用就是反射调用定时任务的
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MethodJobHandler extends IJobHandler {

    /**
     * 目标类对象，就是用户定义的IOC容器中的bean
     */
    private final Object target;
    /**
     * 目标方法，就是要被执行的定时任务方法
     */
    private final Method method;
    /**
     * bean对象的初始化方法
     */
    private Method initMethod;
    /**
     * bean对象的销毁方法
     */
    private Method destroyMethod;

    public MethodJobHandler(Object target, Method method, Method initMethod, Method destroyMethod) {
        this.target = target;
        this.method = method;
        this.initMethod = initMethod;
        this.destroyMethod = destroyMethod;
    }


    /**
     * 通过反射执行定时任务方法
     *
     * @throws Exception
     */
    @Override
    public void execute() throws Exception {
        //获取当前定时任务方法的参数类型合集
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length > 0) {
            //反射调用方法
            method.invoke(target, new Object[paramTypes.length]);
        } else {
            //没有参数，就直接反射调用方法
            method.invoke(target);
        }
    }


    /**
     * 反射调用目标对象的init方法
     *
     * @throws Exception
     */
    @Override
    public void init() throws Exception {
        if (initMethod != null) {
            initMethod.invoke(target);
        }
    }


    /**
     * 反射调用目标对象的destroy方法
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        if (destroyMethod != null) {
            destroyMethod.invoke(target);
        }
    }

}
