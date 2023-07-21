package com.ytrue.rpc.register;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ytrue
 * @date 2023-05-19 15:18
 * @description zk 注册中心
 */
@Slf4j
public class ZookeeperRegistry implements Registry {

    private final CuratorFramework client;

    public ZookeeperRegistry(String zkServerAddress) {
        // 重试策略
        ExponentialBackoffRetry retry = new ExponentialBackoffRetry(1000, 3, 1000);
        // zk链接配置
        this.client = CuratorFrameworkFactory.newClient(zkServerAddress, 1000, 1000, retry);
        // zk 启动
        this.client.start();
    }

    @Override
    public void registerService(String targetInterfaceName, HostAndPort hostAndPort) {
        // 节点路径
        String servicePath = getServicePath(targetInterfaceName);
        try {
            // 判断路径是否存在,不存在递归创建，永久节点
            if (client.checkExists().forPath(servicePath) == null) {
                // /rpc/xxx/provider
                this.client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(servicePath);
            }
            //挂ip:port
            String nodeUrl = this.client.create().withMode(CreateMode.EPHEMERAL).forPath(servicePath + "/" + hostAndPort.getHostName() + ":" + hostAndPort.getPort());
            log.debug("node is created {} ", nodeUrl);
        } catch (Exception e) {
            log.error("node created error ", e);
            throw new RuntimeException("register 节点出现错误...");
        }
    }

    @Override
    public List<HostAndPort> receiveService(String targetInterfaceName) {

        String servicePath = getServicePath(targetInterfaceName);

        try {
            // 判断服务阶段是是否存在，不存在就是没有服务列表
            if (this.client.checkExists().forPath(servicePath) != null) {
                return transferServiceListToHostAndName(this.client.getChildren().forPath(servicePath));
            }

            throw new RuntimeException("没有服务列表....");
        } catch (Exception e) {
            log.error("服务的发现出现问题...", e);
            throw new RuntimeException("服务的发现产生了异常.....");
        }
    }

    /**
     * List<String> ---> List<HostAndPort>
     * 将服务列表转移到主机和名称
     *
     * @param serviceList
     * @return
     */
    private List<HostAndPort> transferServiceListToHostAndName(List<String> serviceList) {
        return serviceList.stream()
                // 按:分割
                .map(s -> s.split(":"))
                .map(sa -> new HostAndPort(sa[0], Integer.parseInt(sa[1])))
                .collect(Collectors.toList());
    }

    @Override
    public void subscribeService(String targetInterfaceName, List<HostAndPort> existingHostAndPort) {
        String servicePath = SERVICE_PREFIX + "/" + targetInterfaceName + SERVICE_SUFFIX;
        CuratorCache curatorCache = CuratorCache.build(client, servicePath);

        // 监听路径变化 /z1/z2/z3/z4 监听子（多级）路径
        CuratorCacheListener curatorCacheListener = CuratorCacheListener.builder().forPathChildrenCache(servicePath, client, (curatorFramework, pathChildrenCacheEvent) -> {
            //1 目前服务列表中的数据清除掉
            existingHostAndPort.clear();
            //2 获取最新的服务列表数据 client.getChildren().forPath(servicePath) --> List<String>
            existingHostAndPort.addAll(transferServiceListToHostAndName(client.getChildren().forPath(servicePath)));
        }).build();

        curatorCache.listenable().addListener(curatorCacheListener);
        curatorCache.start();
    }

    /**
     * 获取servicePath
     *
     * @param targetInterfaceName
     * @return
     */
    private String getServicePath(String targetInterfaceName) {
        return SERVICE_PREFIX + "/" + targetInterfaceName + SERVICE_SUFFIX;
    }
}
