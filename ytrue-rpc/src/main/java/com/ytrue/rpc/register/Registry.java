package com.ytrue.rpc.register;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-05-19 14:54
 * @description Registry
 */
public interface Registry {

    /**
     * 前缀
     */
    String SERVICE_PREFIX = "/rpc";

    /**
     * 后缀
     */
    String SERVICE_SUFFIX = "/provider";

    /**
     * 服务的注册
     *
     * @param targetInterfaceName
     * @param hostAndPort
     */
    void registerService(String targetInterfaceName, HostAndPort hostAndPort);

    /**
     * 获取服务列表 服务发现
     *
     * @param targetInterfaceName
     * @return
     */
    List<HostAndPort> receiveService(String targetInterfaceName);

    /**
     * 服务的订阅
     *
     * @param targetInterfaceName
     * @param existingHostAndPort
     */
    void subscribeService(String targetInterfaceName, List<HostAndPort> existingHostAndPort);
}
