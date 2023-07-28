package com.ytrue.netty.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ytrue
 * @date 2023-07-28 9:50
 * @description 这里面的方法应该都很好理解，都是最基础的几个方法
 */
public abstract class AbstractConstant<T extends AbstractConstant<T>> implements Constant<T> {

    private static final AtomicLong uniqueIdGenerator = new AtomicLong();

    /**
     * id
     */
    private final int id;

    /**
     * 名字
     */
    private final String name;


    private final long uniquifier;

    /**
     * 构造
     *
     * @param id
     * @param name
     */
    protected AbstractConstant(int id, String name) {
        this.id = id;
        this.name = name;
        this.uniquifier = uniqueIdGenerator.getAndIncrement();
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final int id() {
        return id;
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public final int compareTo(T o) {

        // 首先，如果两个对象是同一个对象（引用相等），则返回0，表示两个对象相等。
        if (this == o) {
            return 0;
        }
        AbstractConstant<T> other = o;
        int returnCode;

        // 如果两个对象的hashCode不相等，则将hashCode的差值作为返回值。
        returnCode = hashCode() - other.hashCode();
        if (returnCode != 0) {
            return returnCode;
        }

        // 如果当前对象的uniquifier小于另一个对象的uniquifier，则返回-1，表示当前对象在另一个对象之前。
        if (uniquifier < other.uniquifier) {
            return -1;
        }
        //如果当前对象的uniquifier大于另一个对象的uniquifier，则返回1，表示当前对象在另一个对象之后。
        if (uniquifier > other.uniquifier) {
            return 1;
        }

        // 如果以上条件都不满足，则抛出一个Error异常，表示无法比较两个不同的常量。
        throw new Error("failed to compare two different constants");
    }
}
