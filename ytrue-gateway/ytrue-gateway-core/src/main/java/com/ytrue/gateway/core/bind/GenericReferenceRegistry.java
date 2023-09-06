package com.ytrue.gateway.core.bind;

import com.ytrue.gateway.core.session.Configuration;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-06 14:22
 * @description 泛化调用注册器
 */
public class GenericReferenceRegistry {

    /**
     * 配置类
     */
    private final Configuration configuration;

    /**
     * 泛化调用静态代理工厂
     */
    private final Map<String, GenericReferenceProxyFactory> knownGenericReferences = new HashMap<>();


    public GenericReferenceRegistry(Configuration configuration) {
        this.configuration = configuration;
    }


    /**
     * 获取IGenericReference
     *
     * @param methodName
     * @return
     */
    public IGenericReference getGenericReference(String methodName) {
        GenericReferenceProxyFactory genericReferenceProxyFactory = knownGenericReferences.get(methodName);
        if (genericReferenceProxyFactory == null) {
            throw new RuntimeException("Type " + methodName + " is not known to the GenericReferenceRegistry.");
        }
        return genericReferenceProxyFactory.newInstance(methodName);
    }


    /**
     * 注册泛化调用服务接口方法
     *
     * @param application
     * @param interfaceName
     * @param methodName
     */
    public void addGenericReference(String application, String interfaceName, String methodName) {
        // 获取基础服务（创建成本较高，内存存放获取）
        ApplicationConfig applicationConfig = configuration.getApplicationConfig(application);
        // 获取注册中心配置
        RegistryConfig registryConfig = configuration.getRegistryConfig(application);
        // 首先是基于 ReferenceConfig 定义了订阅的服务信息，包括接口的信息
        // 获取对于的ReferenceConfig<GenericService>,这里就是消费者调用的接口啦
        ReferenceConfig<GenericService> reference = configuration.getReferenceConfig(interfaceName);

        // 构建Dubbo服务
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap
                .application(applicationConfig)
                .registry(registryConfig)
                .reference(reference)
                .start();


        // 获取泛化调用服务
        /*ReferenceConfigCache cache = ReferenceConfigCache.getCache();
        GenericService genericService = cache.get(reference);*/

        GenericService genericService = reference.get();
        // 创建并保存泛化工厂
        knownGenericReferences.put(methodName, new GenericReferenceProxyFactory(genericService));
    }

}
