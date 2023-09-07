package com.ytrue.gateway.core.session;

import com.ytrue.gateway.core.bind.IGenericReference;

import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-06 17:00
 * @description GatewaySession
 */
public interface GatewaySession {

    /**
     * get rpc调用
     *
     * @param methodName
     * @param params
     * @return
     */
    Object get(String methodName, Map<String, Object> params);

    /**
     * post rpc调用
     *
     * @param methodName
     * @param params
     * @return
     */
    Object post(String methodName, Map<String, Object> params);


    /**
     * 获取衍射
     *
     * @param
     * @return
     */
    IGenericReference getMapper();

    /**
     * 获取配置
     *
     * @return
     */
    Configuration getConfiguration();
}
