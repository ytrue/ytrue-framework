package com.ytrue.gateway.core.bind;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.dubbo.rpc.service.GenericService;

import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2023-09-06 14:37
 * @description 泛化调用静态代理：方便做一些拦截处理。给 http 对应的 RPC 调用，做一层代理控制。每调用到一个 http 对应的网关方法，就会代理的方式调用到 RPC 对应的泛化调用方法上
 */
public class GenericReferenceProxy implements MethodInterceptor {


    /**
     * RPC 泛化调用服务
     */
    private final GenericService genericService;
    /**
     * RPC 泛化调用方法
     */
    private final String methodName;

    public GenericReferenceProxy(GenericService genericService, String methodName) {
        this.genericService = genericService;
        this.methodName = methodName;
    }


    /**
     * 做一层代理控制，后续不止是可以使用 Dubbo 泛化调用，也可以是其他服务的泛化调用
     *
     * @param o
     * @param method
     * @param args
     * @param proxy
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        // 获取方法的参数类
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 创建参数名称数组
        String[] parameters = new String[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = parameterTypes[i].getName();
        }
        // 举例：genericService.$invoke("sayHi", new String[]{"java.lang.String"}, new Object[]{"world"});
        return genericService.$invoke(methodName, parameters, args);
    }
}
