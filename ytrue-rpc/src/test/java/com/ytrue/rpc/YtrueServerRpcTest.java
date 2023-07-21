package com.ytrue.rpc;

import com.ytrue.rpc.register.ZookeeperRegistry;
import com.ytrue.rpc.server.RpcServerProvider;
import com.ytrue.rpc.service.OrderService;
import com.ytrue.rpc.service.OrderServiceImpl;

import java.util.HashMap;

/**
 * @author ytrue
 * @date 2023-05-19 12:56
 * @description YtrueServerRpcTest
 */
public class YtrueServerRpcTest {

    public static void main(String[] args) {


        HashMap<String, Object> map = new HashMap<>();
        map.put(OrderService.class.getName(), new OrderServiceImpl());

        ZookeeperRegistry register = new ZookeeperRegistry("127.0.0.1:2181");

        RpcServerProvider rpcServerProvider = new RpcServerProvider(register, map);
        rpcServerProvider.startServer();

    }
}
