package com.ytrue.gateway.core.datasource;

/**
 * @author ytrue
 * @date 2023-09-07 9:23
 * @description 连接接口
 */
public interface Connection {

    /**
     * 执行
     *
     * @param method
     * @param parameterTypes
     * @param parameterNames
     * @param args
     * @return
     */
    Object execute(String method, String[] parameterTypes, String[] parameterNames, Object[] args);
}
