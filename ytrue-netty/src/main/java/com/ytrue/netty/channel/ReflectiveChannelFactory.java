package com.ytrue.netty.channel;

import com.ytrue.netty.util.internal.ObjectUtil;
import com.ytrue.netty.util.internal.StringUtil;

import java.lang.reflect.Constructor;

/**
 * @author ytrue
 * @date 2023-07-26 9:06
 * @description ReflectiveChannelFactory 反射创建channel
 */
public class ReflectiveChannelFactory<T extends Channel> implements ChannelFactory<T> {


    private final Constructor<? extends T> constructor;

    public ReflectiveChannelFactory(Class<? extends T> clazz) {
        ObjectUtil.checkNotNull(clazz, "clazz");
        try {
            // 获取构造
            this.constructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + StringUtil.simpleClassName(clazz) + " does not have a public non-arg constructor", e);
        }
    }


    @Override
    public T newChannel() {
        try {
            // 反射实例化
            return constructor.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Unable to create Channel from class " + constructor.getDeclaringClass(), t);
        }
    }


    @Override
    public String toString() {
        return StringUtil.simpleClassName(ReflectiveChannelFactory.class) +
                '(' + StringUtil.simpleClassName(constructor.getDeclaringClass()) + ".class)";
    }
}
