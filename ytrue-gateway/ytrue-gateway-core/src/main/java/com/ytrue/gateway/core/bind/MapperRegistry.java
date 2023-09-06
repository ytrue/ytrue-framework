package com.ytrue.gateway.core.bind;

import com.ytrue.gateway.core.mapping.HttpStatement;
import com.ytrue.gateway.core.session.Configuration;
import com.ytrue.gateway.core.session.GatewaySession;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-06 17:12
 * @description MapperRegistry
 */
public class MapperRegistry {

    private final Configuration configuration;

    public MapperRegistry(Configuration configuration) {
        this.configuration = configuration;
    }


    /**
     * 泛化调用静态代理工厂
     */
    private final Map<String, MapperProxyFactory> knownMappers = new HashMap<>();


    /**
     * 获取IGenericReference
     *
     * @param uri
     * @param gatewaySession
     * @return
     */
    public IGenericReference getMapper(String uri, GatewaySession gatewaySession) {
        final MapperProxyFactory mapperProxyFactory = knownMappers.get(uri);
        if (mapperProxyFactory == null) {
            throw new RuntimeException("Uri " + uri + " is not known to the MapperRegistry.");
        }
        try {
            return mapperProxyFactory.newInstance(gatewaySession);
        } catch (Exception e) {
            throw new RuntimeException("Error getting mapper instance. Cause: " + e, e);
        }
    }

    /**
     * 添加IGenericReference
     *
     * @param httpStatement
     */
    public void addMapper(HttpStatement httpStatement) {
        String uri = httpStatement.getUri();
        // 如果重复注册则报错
        if (hasMapper(uri)) {
            throw new RuntimeException("Uri " + uri + " is already known to the MapperRegistry.");
        }
        knownMappers.put(uri, new MapperProxyFactory(uri));
        // 保存接口映射信息
        configuration.addHttpStatement(httpStatement);
    }

    /**
     * 判断是否包含IGenericReference
     *
     * @param uri
     * @param <T>
     * @return
     */
    public <T> boolean hasMapper(String uri) {
        return knownMappers.containsKey(uri);
    }

}
