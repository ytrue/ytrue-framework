package com.ytrue.gateway.center.application;

import com.ytrue.gateway.center.domain.docker.model.aggregates.NginxConfig;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description 负载均衡配置服务
 */
public interface ILoadBalancingService {

    void updateNginxConfig(NginxConfig nginxConfig) throws Exception;
}
