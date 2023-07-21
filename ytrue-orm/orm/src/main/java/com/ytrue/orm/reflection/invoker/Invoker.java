package com.ytrue.orm.reflection.invoker;

/**
 * @author ytrue
 * @date 2022/8/19 08:53
 * @description 调用者
 */
public interface Invoker {

    /**
     * 调用方法
     * @param target
     * @param args
     * @return
     * @throws Exception
     */
    Object invoke(Object target, Object[] args) throws Exception;

    /**
     * 获取类型
     * @return
     */
    Class<?> getType();
}
