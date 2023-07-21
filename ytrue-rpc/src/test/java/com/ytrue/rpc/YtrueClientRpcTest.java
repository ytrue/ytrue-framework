package com.ytrue.rpc;

import com.ytrue.rpc.cluster.FailOverCluster;
import com.ytrue.rpc.loadbalance.RandomLoadBalancer;
import com.ytrue.rpc.proxy.JdkProxy;
import com.ytrue.rpc.register.ZookeeperRegistry;
import com.ytrue.rpc.service.OrderService;
import com.ytrue.rpc.transport.NettyTransport;

/**
 * @author ytrue
 * @date 2023-05-26 11:50
 * @description YtrueClientRpcTest
 */
public class YtrueClientRpcTest {
    public static void main(String[] args) {

        JdkProxy jdkProxy = new JdkProxy(OrderService.class);

        // 设计集群
        jdkProxy.setCluster(new FailOverCluster());
        // 设置传输
        jdkProxy.setTransport(new NettyTransport());
        // 设置注册中心
        jdkProxy.setRegistry(new ZookeeperRegistry("127.0.0.1:2181"));
        // 设置负载策略
        jdkProxy.setLoadBalancer(new RandomLoadBalancer());

        // 获取代理
        OrderService orderService = (OrderService) jdkProxy.createProxy();
        System.out.println(orderService.test01("你好呀"));
    }
}
