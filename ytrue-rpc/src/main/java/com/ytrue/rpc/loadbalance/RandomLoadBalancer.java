package com.ytrue.rpc.loadbalance;

import com.ytrue.rpc.register.HostAndPort;

import java.util.List;
import java.util.Random;

/**
 * @author ytrue
 * @date 2023-05-19 14:58
 * @description 随机负载
 */
public class RandomLoadBalancer implements LoadBalancer {

    private final Random random = new Random();

    @Override
    public HostAndPort select(List<HostAndPort> hostAndPorts) {
        if (hostAndPorts == null || hostAndPorts.size() == 0) {
            throw new RuntimeException("hostAndNames set null");
        }
        // 随机数
        int index = random.nextInt(hostAndPorts.size());
        return hostAndPorts.get(index);
    }
}
