package com.ytrue.orm.plugin;

/**
 * @author ytrue
 * @date 2022/9/6 16:33
 * @description Signature
 */
public @interface Signature {

    /**
     * 被拦截类
     */
    Class<?> type();

    /**
     * 被拦截类的方法
     */
    String method();

    /**
     * 被拦截类的方法的参数
     */
    Class<?>[] args();
}
