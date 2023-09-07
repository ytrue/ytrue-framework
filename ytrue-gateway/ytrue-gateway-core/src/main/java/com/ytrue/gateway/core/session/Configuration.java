package com.ytrue.gateway.core.session;

import com.ytrue.gateway.core.authorization.IAuth;
import com.ytrue.gateway.core.bind.IGenericReference;
import com.ytrue.gateway.core.bind.MapperRegistry;
import com.ytrue.gateway.core.datasource.Connection;
import com.ytrue.gateway.core.executor.Executor;
import com.ytrue.gateway.core.executor.SimpleExecutor;
import com.ytrue.gateway.core.mapping.HttpStatement;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.service.GenericService;
import com.ytrue.gateway.core.authorization.auth.AuthService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-06 16:57
 * @description Configuration
 */
public class Configuration {

    /**
     * 网关 Netty 服务地址
     */
    private String hostName = "127.0.0.1";

    /**
     * 网关 Netty 服务端口
     */
    private int port = 7397;

    /**
     * 网关 Netty 服务线程数配置
     */
    private int bossNThreads = 1;
    private int workNThreads = 4;


    private final MapperRegistry mapperRegistry = new MapperRegistry(this);

    private final Map<String, HttpStatement> httpStatements = new HashMap<>();

    /**
     * RPC 应用服务配置项 api-gateway-test
     */
    private final Map<String, ApplicationConfig> applicationConfigMap = new HashMap<>();
    /**
     * RPC 注册中心配置项 zookeeper://127.0.0.1:2181
     */
    private final Map<String, RegistryConfig> registryConfigMap = new HashMap<>();
    /**
     * RPC 泛化服务配置项 cn.bugstack.gateway.rpc.IActivityBooth
     */
    private final Map<String, ReferenceConfig<GenericService>> referenceConfigMap = new HashMap<>();


    private final IAuth auth = new AuthService();


    public Configuration() {

    }

    public synchronized void registryConfig(String applicationName, String address, String interfaceName, String version) {
        if (applicationConfigMap.get(applicationName) == null) {
            ApplicationConfig application = new ApplicationConfig();
            application.setName(applicationName);
            application.setQosEnable(false);
            applicationConfigMap.put(applicationName, application);
        }

        if (registryConfigMap.get(applicationName) == null) {
            RegistryConfig registry = new RegistryConfig();
            registry.setAddress(address);
            registry.setRegister(false);
            registryConfigMap.put(applicationName, registry);
        }

        if (referenceConfigMap.get(interfaceName) == null) {
            ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
            reference.setInterface(interfaceName);
            reference.setVersion(version);
            reference.setGeneric("true");
            referenceConfigMap.put(interfaceName, reference);
        }
    }


    //-------------------------------------new xxxx
    public Executor newExecutor(Connection connection) {
        return new SimpleExecutor(this, connection);
    }

    public boolean authValidate(String uId, String token) {
        return auth.validate(uId, token);
    }

    //------------------------get and set
    public ApplicationConfig getApplicationConfig(String applicationName) {
        return applicationConfigMap.get(applicationName);
    }

    public RegistryConfig getRegistryConfig(String applicationName) {
        return registryConfigMap.get(applicationName);
    }

    public ReferenceConfig<GenericService> getReferenceConfig(String interfaceName) {
        return referenceConfigMap.get(interfaceName);
    }

    public void addMapper(HttpStatement httpStatement) {
        mapperRegistry.addMapper(httpStatement);
    }

    public IGenericReference getMapper(String uri, GatewaySession gatewaySession) {
        return mapperRegistry.getMapper(uri, gatewaySession);
    }

    public void addHttpStatement(HttpStatement httpStatement) {
        httpStatements.put(httpStatement.getUri(), httpStatement);
    }

    public HttpStatement getHttpStatement(String uri) {
        return httpStatements.get(uri);
    }



    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getBossNThreads() {
        return bossNThreads;
    }

    public void setBossNThreads(int bossNThreads) {
        this.bossNThreads = bossNThreads;
    }

    public int getWorkNThreads() {
        return workNThreads;
    }

    public void setWorkNThreads(int workNThreads) {
        this.workNThreads = workNThreads;
    }
}
