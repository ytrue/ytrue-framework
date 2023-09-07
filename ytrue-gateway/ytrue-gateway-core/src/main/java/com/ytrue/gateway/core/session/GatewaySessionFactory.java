package com.ytrue.gateway.core.session;

/**
 * @author ytrue
 * @date 2023-09-06 17:01
 * @description GatewaySessionFactory
 */
public interface GatewaySessionFactory {

    /**
     * 创建GatewaySession
     *
     * @param uri
     * @return
     */
    GatewaySession openSession(String uri);
}
