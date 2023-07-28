package com.ytrue.netty.util;

import com.ytrue.netty.util.internal.ObjectUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ytrue
 * @date 2023-07-28 9:58
 * @description 常量池，其实是个ConcurrentMap，以key-value的方式存储用户定义的配置信息
 */
public abstract class ConstantPool<T extends Constant<T>> {

    /**
     * 载体
     */
    private final ConcurrentMap<String, T> constants = new ConcurrentHashMap<>();


    /**
     * 初始化常量类的id，初值为1
     */
    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * 创建常量类的方法
     *
     * @param firstNameComponent  第一个参数
     * @param secondNameComponent 第二个参数
     * @return
     */
    public T valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        // 空校验
        if (firstNameComponent == null) {
            throw new NullPointerException("firstNameComponent");
        }

        // 空校验
        if (secondNameComponent == null) {
            throw new NullPointerException("secondNameComponent");
        }

        return valueOf(firstNameComponent.getName() + '#' + secondNameComponent);
    }

    /**
     * 创建常量类的方法
     *
     * @param name
     * @return
     */
    public T valueOf(String name) {
        checkNotNullAndNotEmpty(name);
        return getOrCreate(name);
    }

    /**
     * 真正创建常量类的方法，这里的参数就是常量的名字，创建的常量是以key-name，value-ChannelOption<T>的形式
     *
     * @param name
     * @return
     */
    private T getOrCreate(String name) {
        T constant = constants.get(name);
        //先判断常量池中是否有该常量
        if (constant == null) {
            //没有的话就创建
            final T tempConstant = newConstant(nextId(), name);
            //然后放进常量池中
            // 1. 如果指定的键在Map中不存在，则将指定的键值对添加到Map中，并返回null。
            // 2. 如果指定的键在Map中存在，并且对应的值为null，则将指定的值与该键关联，并返回null。
            // 3. 如果指定的键在Map中存在，并且对应的值不为null，则不会进行任何操作，并返回该键对应的值。
            constant = constants.putIfAbsent(name, tempConstant);
            if (constant == null) {
                return tempConstant;
            }
        }
        //最后返回该常量
        return constant;
    }

    /**
     * 也是创建常量的方法，只不过这里调用了另一个创建方法
     *
     * @param name
     * @return
     */
    public T newInstance(String name) {
        checkNotNullAndNotEmpty(name);
        return createOrThrow(name);
    }

    /**
     * 该方法和getOrCreate并没什么区别，只不过该方法会在创建失败后抛出异常，表示该常量已经在使用了
     *
     * @param name
     * @return
     */

    private T createOrThrow(String name) {
        T constant = constants.get(name);
        if (constant == null) {
            final T tempConstant = newConstant(nextId(), name);
            constant = constants.putIfAbsent(name, tempConstant);
            if (constant == null) {
                return tempConstant;
            }
        }
        throw new IllegalArgumentException(String.format("'%s' is already in use", name));
    }


    /**
     * 校验是否为空
     *
     * @param name
     * @return
     */
    private static String checkNotNullAndNotEmpty(String name) {
        ObjectUtil.checkNotNull(name, "name");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("empty name");
        }
        return name;
    }


    /**
     * 判断key 是否存在
     *
     * @param name
     * @return
     */
    public boolean exists(String name) {
        checkNotNullAndNotEmpty(name);
        return constants.containsKey(name);
    }


    /**
     * 终于又看到一个抽象方法了，把该方法设置为抽象的，立刻就应该想到该抽象类有不止一个实现子类，ChannelOption只是其中之一
     *
     * @param id
     * @param name
     * @return
     */
    protected abstract T newConstant(int id, String name);

    /**
     * 该方法虽然被标注为废弃的了，但是仍然在被使用，所以我也没有删掉它
     *
     * @return
     */
    @Deprecated
    public final int nextId() {
        return nextId.getAndIncrement();
    }
}
