package com.ytrue.rpc.proxy;

import com.ytrue.rpc.cluster.Cluster;
import com.ytrue.rpc.loadbalance.LoadBalancer;
import com.ytrue.rpc.protocol.RpcRequest;
import com.ytrue.rpc.protocol.RpcResponse;
import com.ytrue.rpc.register.HostAndPort;
import com.ytrue.rpc.register.Registry;
import com.ytrue.rpc.transport.Transport;
import com.ytrue.rpc.utils.ClassLoaderUtils;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-05-19 16:36
 * @description JdkProxy
 */
public class JdkProxy implements InvocationHandler {

    /**
     * 目标接口
     */
    @Getter
    private Class<?> targetInterface;

    /**
     * 集群
     */
    @Setter
    private Cluster cluster;

    /**
     * 负载策略
     */
    @Setter
    @Getter
    private LoadBalancer loadBalancer;

    /**
     * 传输
     */
    @Setter
    @Getter
    private Transport transport;

    /**
     * 注册中心
     */
    @Setter
    @Getter
    private Registry registry;


    /**
     * 服务列表
     */
    @Getter
    private List<HostAndPort> hostAndPorts;


    public JdkProxy(Class<?> targetInterface) {
        this.targetInterface = targetInterface;
    }

    /**
     * 创建代理
     *
     * @return
     */
    public Object createProxy() {
        // 从注册中心中发现服务列表
        hostAndPorts = registry.receiveService(targetInterface.getName());
        // 服务列表的订阅
        registry.subscribeService(targetInterface.getName(), hostAndPorts);
        // 创建代理对象
        return Proxy.newProxyInstance(ClassLoaderUtils.getClassLoader(targetInterface), new Class[]{targetInterface}, this);
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 封装请求传输
        RpcRequest rpcRequest = new RpcRequest(targetInterface, method.getName(), method.getParameterTypes(), args);

        // 调用
        RpcResponse result = cluster.invoke(hostAndPorts, loadBalancer, transport, rpcRequest);

        // 判断是否有异常
        if (result.getException() != null) {
            throw result.getException();
        }

        // 返回结果
        return result.getResultValue();
    }

}
