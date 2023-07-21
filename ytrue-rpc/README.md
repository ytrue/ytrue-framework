# ytrue-rpc

实现简单rpc

# 服务端使用方式

```java
public class YtrueServerRpcTest {

    public static void main(String[] args) {
        // 设置对外暴露服务
        HashMap<String, Object> map = new HashMap<>();
        map.put(OrderService.class.getName(), new OrderServiceImpl());

        // 设置注册中心
        ZookeeperRegistry register = new ZookeeperRegistry("127.0.0.1:2181");

        RpcServerProvider rpcServerProvider = new RpcServerProvider(register, map);
        // 启动服务
        rpcServerProvider.startServer();
    }
}
```

# 客户端使用方式

```java
public class YtrueClientRpcTest {
    public static void main(String[] args) {
        JdkProxy jdkProxy = new JdkProxy(OrderService.class);
        // 设置集群，支持快速失败，快速跳过
        jdkProxy.setCluster(new FailOverCluster());
        // 设置传输
        jdkProxy.setTransport(new NettyTransport());
        // 设置注册中心
        jdkProxy.setRegistry(new ZookeeperRegistry("127.0.0.1:2181"));
        // 设置负载策略
        jdkProxy.setLoadBalancer(new RandomLoadBalancer());

        // 获取代理
        OrderService orderService = (OrderService) jdkProxy.createProxy();
        System.out.println(orderService.test01("你好呀"));
    }
}
```

# 后续

1. 与spring整合，支持自定义标签方式，注解方式
2. 负载策略待完善
3. nacos注册中心完善
