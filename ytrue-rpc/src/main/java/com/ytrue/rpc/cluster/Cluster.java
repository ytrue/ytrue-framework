package com.ytrue.rpc.cluster;

import com.ytrue.rpc.loadbalance.LoadBalancer;
import com.ytrue.rpc.protocol.RpcRequest;
import com.ytrue.rpc.protocol.RpcResponse;
import com.ytrue.rpc.register.HostAndPort;
import com.ytrue.rpc.transport.Transport;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-05-20 13:44
 * @description Cluster
 */
public interface Cluster {

    /**
     * 集群调用
     *
     * @param hostAndPorts 地址列表
     * @param loadBalancer 负载策略
     * @param transport    客户端
     * @param request      请求参数
     * @return RpcResponse 响应参数
     */
    public RpcResponse invoke(List<HostAndPort> hostAndPorts, LoadBalancer loadBalancer, Transport transport, RpcRequest request);
}
