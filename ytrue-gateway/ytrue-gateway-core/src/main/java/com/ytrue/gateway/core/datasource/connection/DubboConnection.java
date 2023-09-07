package com.ytrue.gateway.core.datasource.connection;


import com.ytrue.gateway.core.datasource.Connection;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.service.GenericService;

/**
 * @author ytrue
 * @date 2023-09-07 9:25
 * @description DubboConnection
 */
public class DubboConnection implements Connection {

    private final GenericService genericService;

    /**
     * 构造
     *
     * @param applicationConfig
     * @param registryConfig
     * @param reference
     */
    public DubboConnection(
            ApplicationConfig applicationConfig,
            RegistryConfig registryConfig,
            ReferenceConfig<GenericService> reference
    ) {
        // 连接远程服务
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(applicationConfig).registry(registryConfig).reference(reference).start();
        // 获取泛化接口
        genericService = reference.get();
    }

    @Override
    public Object execute(String method, String[] parameterTypes, String[] parameterNames, Object[] args) {
        return genericService.$invoke(method, parameterTypes, args);
    }
}
