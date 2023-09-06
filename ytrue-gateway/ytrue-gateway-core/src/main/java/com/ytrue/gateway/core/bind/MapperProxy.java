package com.ytrue.gateway.core.bind;

import com.ytrue.gateway.core.session.GatewaySession;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2023-09-06 17:10
 * @description MapperProxy
 */
public class MapperProxy implements MethodInterceptor {

    private GatewaySession gatewaySession;
    private final String uri;

    public MapperProxy(GatewaySession gatewaySession, String uri) {
        this.gatewaySession = gatewaySession;
        this.uri = uri;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        MapperMethod linkMethod = new MapperMethod(uri, method, gatewaySession.getConfiguration());
        return linkMethod.execute(gatewaySession, args);
    }
}
