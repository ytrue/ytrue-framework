package com.ytrue.gateway.core.session;

import com.ytrue.gateway.core.bind.GenericReferenceRegistry;
import com.ytrue.gateway.core.bind.IGenericReference;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.service.GenericService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-06 14:21
 * @description 会话生命周期配置项
 */
public class Configuration {


    /**
     * 泛化调用注册器
     */
    private final GenericReferenceRegistry registry = new GenericReferenceRegistry(this);


    /**
     * DUBBO 应用服务配置项 api-gateway-test
     */
    private final Map<String, ApplicationConfig> applicationConfigMap = new HashMap<>();

    /**
     * DUBBO 注册中心配置项 zookeeper://127.0.0.1:2181
     */
    private final Map<String, RegistryConfig> registryConfigMap = new HashMap<>();

    /**
     * DUBBO 泛化服务配置项 com.ytrue.gateway.rpc.IActivityBooth
     */
    private final Map<String, ReferenceConfig<GenericService>> referenceConfigMap = new HashMap<>();


    public Configuration() {
        // dubbo 的基本配置
        ApplicationConfig application = new ApplicationConfig();
        application.setName("api-gateway-test");
        application.setQosEnable(false);

        // 注册中心配置
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress("zookeeper://127.0.0.1:2181");
        registry.setRegister(false);

        // 泛化服务配置项
        ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
        reference.setInterface("cn.bugstack.gateway.rpc.IActivityBooth");
        reference.setVersion("1.0.0");
        reference.setGeneric("true");

        applicationConfigMap.put("api-gateway-test", application);
        registryConfigMap.put("api-gateway-test", registry);
        referenceConfigMap.put("cn.bugstack.gateway.rpc.IActivityBooth", reference);
    }


    // ------------------------------------ GET  AND SET
    public ApplicationConfig getApplicationConfig(String applicationName) {
        return applicationConfigMap.get(applicationName);
    }

    public RegistryConfig getRegistryConfig(String applicationName) {
        return registryConfigMap.get(applicationName);
    }

    public ReferenceConfig<GenericService> getReferenceConfig(String interfaceName) {
        return referenceConfigMap.get(interfaceName);
    }

    public void addGenericReference(String application, String interfaceName, String methodName) {
        registry.addGenericReference(application, interfaceName, methodName);
    }

    public IGenericReference getGenericReference(String methodName) {
        return registry.getGenericReference(methodName);
    }
}
