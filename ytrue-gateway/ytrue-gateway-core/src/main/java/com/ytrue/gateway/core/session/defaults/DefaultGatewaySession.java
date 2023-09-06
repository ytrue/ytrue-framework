package com.ytrue.gateway.core.session.defaults;

import com.ytrue.gateway.core.bind.IGenericReference;
import com.ytrue.gateway.core.mapping.HttpStatement;
import com.ytrue.gateway.core.session.Configuration;
import com.ytrue.gateway.core.session.GatewaySession;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.rpc.service.GenericService;

/**
 * @author ytrue
 * @date 2023-09-06 17:03
 * @description DefaultGatewaySession
 */
public class DefaultGatewaySession implements GatewaySession {

    private Configuration configuration;

    public DefaultGatewaySession(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Object get(String uri, Object parameter) {
        /* 以下这部分内容，后续拆到执行器中处理 */

        // 配置信息
        HttpStatement httpStatement = configuration.getHttpStatement(uri);
        String application = httpStatement.getApplication();
        String interfaceName = httpStatement.getInterfaceName();

        // 获取基础服务（创建成本较高，内存存放获取）
        ApplicationConfig applicationConfig = configuration.getApplicationConfig(application);
        RegistryConfig registryConfig = configuration.getRegistryConfig(application);
        ReferenceConfig<GenericService> reference = configuration.getReferenceConfig(interfaceName);
        // 构建Dubbo服务
        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap.application(applicationConfig).registry(registryConfig).reference(reference).start();

        // 获取泛化调用服务
        GenericService genericService = reference.get();

        return genericService.$invoke(httpStatement.getMethodName(), new String[]{"java.lang.String"}, new Object[]{"小傅哥"});
    }

    @Override
    public IGenericReference getMapper(String uri) {
        return configuration.getMapper(uri, this);
    }

    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }
}
