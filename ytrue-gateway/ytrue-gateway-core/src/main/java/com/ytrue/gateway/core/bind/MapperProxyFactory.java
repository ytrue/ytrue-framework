package com.ytrue.gateway.core.bind;

import com.ytrue.gateway.core.mapping.HttpStatement;
import com.ytrue.gateway.core.session.GatewaySession;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InterfaceMaker;
import org.objectweb.asm.Type;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ytrue
 * @date 2023-09-06 17:07
 * @description MapperProxyFactory
 */
public class MapperProxyFactory {
    /**
     * 接口url
     */
    private String uri;

    /**
     * 构造
     *
     * @param uri
     */
    public MapperProxyFactory(String uri) {
        this.uri = uri;
    }

    /**
     * 缓存
     */
    private final Map<String, IGenericReference> genericReferenceCache = new ConcurrentHashMap<>();


    public IGenericReference newInstance(GatewaySession gatewaySession) {
        return genericReferenceCache.computeIfAbsent(uri, k -> {
            HttpStatement httpStatement = gatewaySession.getConfiguration().getHttpStatement(uri);
            // 泛化调用
            MapperProxy genericReferenceProxy = new MapperProxy(gatewaySession, uri);
            // 创建接口
            InterfaceMaker interfaceMaker = new InterfaceMaker();
            // 使用 add() 方法向 InterfaceMaker 添加一个方法签名。
            // 这里的 method 是一个 Method 对象，它定义了方法的名称、返回类型和参数类型。
            // Signature 对象表示方法的签名，其中包含了方法的名称、返回类型和参数类型的信息。第二个参数 null 是方法体（Body），在这个例子中为 null，表示没有具体的实现。
            interfaceMaker.add(new Signature(
                    // 创建的方法
                    httpStatement.getMethodName(),
                    // 方法的返回类型
                    Type.getType(String.class),
                    // 方法的参数
                    new Type[]{Type.getType(String.class)}), null);

            // create() 方法，动态地生成一个接口类，并将生成的接口类存储在 interfaceClass 变量中
            Class<?> interfaceClass = interfaceMaker.create();

            // 代理对象
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(Object.class);
            // IGenericReference 统一泛化调用接口
            // interfaceClass    根据泛化调用注册信息创建的接口，建立 http -> rpc 关联
            // 使用 Enhancer 创建一个代理对象。
            // 设置代理对象的父类为 Object，并将动态生成的接口类（interfaceClass）和 IGenericReference 接口作为代理对象的接口。
            enhancer.setInterfaces(new Class[]{IGenericReference.class, interfaceClass});
            // 代理回调
            enhancer.setCallback(genericReferenceProxy);
            return (IGenericReference) enhancer.create();
        });
    }

}
