package com.ytrue.rpc.cluster;

import com.ytrue.rpc.loadbalance.LoadBalancer;
import com.ytrue.rpc.protocol.RpcRequest;
import com.ytrue.rpc.protocol.RpcResponse;
import com.ytrue.rpc.register.HostAndPort;
import com.ytrue.rpc.transport.Transport;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-05-20 13:45
 * @description FailFastCluster 快速失败
 */
@Slf4j
public class FailFastCluster implements Cluster {
    @Override
    public RpcResponse invoke(List<HostAndPort> hostAndPorts, LoadBalancer loadBalancer, Transport transport, RpcRequest request) {
        HostAndPort hostAndPort = loadBalancer.select(hostAndPorts);
        RpcResponse result;
        try {
            result = transport.invoke(hostAndPort, request);
            transport.close();
        } catch (Exception e) {
            log.error("集群调用产生错误..使用FailFast的方式进行容错....");
            transport.close();
            throw new RuntimeException(e);
        }
        return result;
    }
}
