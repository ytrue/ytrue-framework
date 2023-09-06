package com.ytrue.gateway.core.session;

import com.ytrue.gateway.core.bind.IGenericReference;

/**
 * @author ytrue
 * @date 2023-09-06 17:00
 * @description GatewaySession
 */
public interface GatewaySession {

    /**
     * rpc调用
     *
     * @param uri
     * @param parameter
     * @return
     */
    Object get(String uri, Object parameter);


    /**
     * 获取衍射
     *
     * @param uri
     * @return
     */
    IGenericReference getMapper(String uri);

    /**
     * 获取配置
     *
     * @return
     */
    Configuration getConfiguration();
}
