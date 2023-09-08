package com.ytrue.gateway.center.domain.docker.service;


import com.ytrue.gateway.center.application.ILoadBalancingService;
import com.ytrue.gateway.center.domain.docker.model.aggregates.NginxConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author 小傅哥，微信：fustack
 * @description 负载均衡抽象类
 * @github https://github.com/fuzhengwei
 * @Copyright 公众号：bugstack虫洞栈 | 博客：https://bugstack.cn - 沉淀、分享、成长，让自己和他人都能有所收获！
 */
public abstract class AbstractLoadBalancing implements ILoadBalancingService {

    private Logger logger = LoggerFactory.getLogger(AbstractLoadBalancing.class);

    @Override
    public void updateNginxConfig(NginxConfig nginxConfig) throws Exception {
        // 1. 创建 Nginx 配置文件
        String containerFilePath = createNginxConfigFile(nginxConfig);
        logger.info("步骤1：创建 Nginx 配置文件 containerFilePath：{}", containerFilePath);
        // 2. 复制 Nginx 配置文件
        // copyDockerFile(nginxConfig.getApplicationName(), containerFilePath, nginxConfig.getLocalNginxPath());
        // logger.info("步骤2：拷贝 Nginx 配置文件 localPath：{}", nginxConfig.getLocalNginxPath());
        // 3. 刷新 Nginx 配置文件
        refreshNginxConfig(nginxConfig.getNginxName());
        logger.info("步骤2：刷新 Nginx 配置文件 Done！");
    }

    protected abstract String createNginxConfigFile(NginxConfig nginxConfig) throws IOException;

    protected abstract void copyDockerFile(String applicationName, String containerFilePath, String localNginxPath) throws InterruptedException, IOException;

    protected abstract void refreshNginxConfig(String nginxName) throws InterruptedException, IOException;

}
