package com.ytrue.netty.util;

/**
 * @author ytrue
 * @date 2023-07-28 9:50
 * @description 常量类的顶级接口，定义了常量的id和名字
 */
public interface Constant<T extends Constant<T>> extends Comparable<T> {

    /**
     * id
     *
     * @return
     */
    int id();

    /**
     * 名字
     *
     * @return
     */
    String name();
}
