package com.ytrue.rpc.cluster;

import com.ytrue.rpc.loadbalance.LoadBalancer;
import com.ytrue.rpc.protocol.RpcRequest;
import com.ytrue.rpc.protocol.RpcResponse;
import com.ytrue.rpc.register.HostAndPort;
import com.ytrue.rpc.transport.NettyTransport;
import com.ytrue.rpc.transport.Transport;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-05-26 11:39
 * @description FailOverCluster
 */
@Slf4j
public class FailOverCluster implements Cluster {
    @Override
    public RpcResponse invoke(List<HostAndPort> hostAndPorts, LoadBalancer loadBalancer, Transport transport, RpcRequest request) {
        HostAndPort hostAndPort = loadBalancer.select(hostAndPorts);
        log.debug("访问的IP {} ", hostAndPort.getPort());
        RpcResponse result;
        try {
            result = transport.invoke(hostAndPort, request);
            transport.close();
        } catch (Exception e) {
            log.error("集群调用产生错误 使用FailOver容错 ", e);
            transport.close();

            // 上一步hostAndPort出问题，从List取其他的HostAndPort进行访问。
            hostAndPorts.remove(hostAndPort);
            if (hostAndPorts.size() == 0) {
                throw new RuntimeException("集群出现错误....");
            } else {
                // 重新调用
                return invoke(hostAndPorts, loadBalancer, new NettyTransport(), request);
            }
        }
        return result;
    }
}
