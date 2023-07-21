package com.ytrue.rpc.loadbalance;

import com.ytrue.rpc.register.HostAndPort;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-05-19 14:57
 * @description 负载策略
 */
public interface LoadBalancer {

    /**
     * 选择
     *
     * @param hostAndPorts
     * @return
     */
    HostAndPort select(List<HostAndPort> hostAndPorts);
}
