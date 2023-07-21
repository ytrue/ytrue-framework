package com.ytrue.rpc.register;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-05-19 15:18
 * @description nacos 注册中心
 */
public class NaosRegistry implements Registry {

    @Override
    public void registerService(String targetInterfaceName, HostAndPort hostAndPort) {

    }

    @Override
    public List<HostAndPort> receiveService(String targetInterfaceName) {
        return null;
    }

    @Override
    public void subscribeService(String targetInterfaceName, List<HostAndPort> existingHostAndPort) {

    }
}
