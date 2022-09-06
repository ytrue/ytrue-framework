package com.ytrue.orm.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2022/9/6 16:40
 * @description 调用信息
 */
@Getter
@AllArgsConstructor
public class Invocation {

    /**
     * 调用的对象
     */
    private Object target;

    /**
     * 调用的方法
     */
    private Method method;

    /**
     * 调用的参数
     */
    private Object[] args;

    /**
     * 放行；调用执行
     *
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public Object proceed() throws InvocationTargetException, IllegalAccessException {
        return method.invoke(target, args);
    }
}
