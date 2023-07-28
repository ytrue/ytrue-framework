package com.ytrue.netty.util;

/**
 * @author ytrue
 * @date 2023-07-28 13:44
 * @description 该类也是一个常量类，把map的key包装为常量
 */
public class AttributeKey<T> extends AbstractConstant<AttributeKey<T>> {



    private static final ConstantPool<AttributeKey<Object>> pool = new ConstantPool<AttributeKey<Object>>() {
        @Override
        protected AttributeKey<Object> newConstant(int id, String name) {
            return new AttributeKey<>(id, name);
        }
    };

    /**
     * 构造
     *
     * @param id
     * @param name
     */
    public AttributeKey(int id, String name) {
        super(id, name);
    }


    /**
     * 创建key的方法
     * @param name
     * @return
     * @param <T>
     */
    public static <T> AttributeKey<T> valueOf(String name) {
        return (AttributeKey<T>) pool.valueOf(name);
    }

    public static boolean exists(String name) {
        return pool.exists(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> AttributeKey<T> newInstance(String name) {
        return (AttributeKey<T>) pool.newInstance(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> AttributeKey<T> valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return (AttributeKey<T>) pool.valueOf(firstNameComponent, secondNameComponent);
    }

}
