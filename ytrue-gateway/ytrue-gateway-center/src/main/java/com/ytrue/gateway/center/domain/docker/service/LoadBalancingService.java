package com.ytrue.gateway.center.domain.docker.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.ytrue.gateway.center.domain.docker.model.aggregates.NginxConfig;
import com.ytrue.gateway.center.domain.docker.model.vo.LocationVO;
import com.ytrue.gateway.center.domain.docker.model.vo.UpstreamVO;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 小傅哥，微信：fustack
 * @description 负载均衡配置服务
 * @github https://github.com/fuzhengwei
 * @Copyright 公众号：bugstack虫洞栈 | 博客：https://bugstack.cn - 沉淀、分享、成长，让自己和他人都能有所收获！
 */
@Service
public class LoadBalancingService extends AbstractLoadBalancing {

    private Logger logger = LoggerFactory.getLogger(LoadBalancingService.class);

    @Value("${nginx.server_name}")
    private String nginx_server_name;

    @Override
    protected String createNginxConfigFile(NginxConfig nginxConfig) throws IOException {
        // 创建文件
        String nginxConfigContentStr = buildNginxConfig(nginxConfig.getUpstreamList(), nginxConfig.getLocationList());
        File file = new File("/data/nginx/nginx.conf");
        if (!file.exists()) {
            boolean success = file.createNewFile();
            if (success) {
                logger.info("nginx.conf file created successfully.");
            } else {
                logger.info("nginx.conf file already exists.");
            }
        }
        // 写入内容
        FileWriter writer = new FileWriter(file);
        writer.write(nginxConfigContentStr);
        writer.close();
        // 返回结果
        return file.getAbsolutePath();
    }

    /**
     * 拷贝容器文件到本地案例；https://github.com/docker-java/docker-java/issues/991
     */
    @Override
    protected void copyDockerFile(String applicationName, String containerFilePath, String localNginxPath) throws InterruptedException, IOException {
        // Docker client
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock").build();

        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        // Copy file from container
        try (TarArchiveInputStream tarStream = new TarArchiveInputStream(
                dockerClient.copyArchiveFromContainerCmd(applicationName,
                        containerFilePath).exec())) {
            unTar(tarStream, new File(localNginxPath));
        }
        dockerClient.close();
    }

    public static void unTar(TarArchiveInputStream tis, File destFile)
            throws IOException {
        TarArchiveEntry tarEntry = null;
        while ((tarEntry = tis.getNextTarEntry()) != null) {
            if (tarEntry.isDirectory()) {
                if (!destFile.exists()) {
                    destFile.mkdirs();
                }
            } else {
                FileOutputStream fos = new FileOutputStream(destFile);
                IOUtils.copy(tis, fos);
                fos.close();
            }
        }
        tis.close();
    }

    @Override
    protected void refreshNginxConfig(String nginxName) throws InterruptedException, IOException {
        // Docker client
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock").build();

        DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

        String containerId = dockerClient.listContainersCmd()
                .withNameFilter(new ArrayList<String>() {{
                    add(nginxName);
                }})
                .exec()
                .get(0)
                .getId();

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient
                .execCreateCmd(containerId)
                .withCmd("nginx", "-s", "reload")
                .exec();

        dockerClient.execStartCmd(execCreateCmdResponse.getId())
                .exec(new ResultCallback.Adapter<>()).awaitCompletion();

        dockerClient.close();
    }

    private String buildNginxConfig(List<UpstreamVO> upstreamList, List<LocationVO> locationList) {
        String config = "\n" +
                "user  nginx;\n" +
                "worker_processes  auto;\n" +
                "\n" +
                "error_log  /var/log/nginx/error.log notice;\n" +
                "pid        /var/run/nginx.pid;\n" +
                "\n" +
                "\n" +
                "events {\n" +
                "    worker_connections  1024;\n" +
                "}\n" +
                "\n" +
                "\n" +
                "http {\n" +
                "    include       /etc/nginx/mime.types;\n" +
                "    default_type  application/octet-stream;\n" +
                "\n" +
                "    log_format  main  '$remote_addr - $remote_user [$time_local] \"$request\" '\n" +
                "                      '$status $body_bytes_sent \"$http_referer\" '\n" +
                "                      '\"$http_user_agent\" \"$http_x_forwarded_for\"';\n" +
                "\n" +
                "    access_log  /var/log/nginx/access.log  main;\n" +
                "\n" +
                "    sendfile        on;\n" +
                "    #tcp_nopush     on;\n" +
                "\n" +
                "    keepalive_timeout  65;\n" +
                "\n" +
                "    #gzip  on;\n" +
                "\n" +
                "    include /etc/nginx/conf.d/*.conf;\n" +
                "\n" +
                "    # 设定负载均衡的服务器列表 命令：docker exec Nginx nginx -s reload\n" +
                "upstream_config_placeholder" +
                "\n" +
                "    # HTTP服务器\n" +
                "    server {\n" +
                "        # 监听80端口，用于HTTP协议\n" +
                "        listen  80;\n" +
                "\n" +
                "        # 定义使用IP/域名访问\n" +
                "        server_name "+nginx_server_name+";\n" +
                "\n" +
                "        # 首页\n" +
                "        index index.html;\n" +
                "\n" +
                "        # 反向代理的路径（upstream绑定），location 后面设置映射的路径\n" +
                "        # location / {\n" +
                "        #    proxy_pass http://192.168.1.102:9001;\n" +
                "        # }\n" +
                "\n" +
                "location_config_placeholder" +
                "    }\n" +
                "}\n";

        // 组装配置 Upstream
        StringBuilder upstreamStr = new StringBuilder();
        for (UpstreamVO upstream : upstreamList) {
            upstreamStr.append("\t").append("upstream").append(" ").append(upstream.getName()).append(" {\r\n");
            upstreamStr.append("\t").append("\t").append(upstream.getStrategy()).append("\r\n").append("\r\n");
            List<String> servers = upstream.getServers();
            for (String server : servers) {
                upstreamStr.append("\t").append("\t").append("server").append(" ").append(server).append(";\r\n");
            }
            upstreamStr.append("\t").append("}").append("\r\n").append("\r\n");
        }

        // 组装配置 Location
        StringBuilder locationStr = new StringBuilder();
        for (LocationVO location : locationList) {
            // location /api01/
            locationStr.append("\t").append("\t").append("location").append(" ").append(location.getName()).append(" {\r\n");
            // rewrite ^/api01/(.*)$ /$1 break; 设置重写URL，在代理后去掉根路径 api01 此字段只是配合路由，不做处理
            locationStr.append("\t").append("\t").append("\t").append("rewrite ^").append(location.getName()).append("(.*)$ /$1 break;").append("\r\n");
            // proxy_pass http://api01;
            locationStr.append("\t").append("\t").append("\t").append("proxy_pass").append(" ").append(location.getProxy_pass()).append("\r\n");
            locationStr.append("\t").append("\t").append("}").append("\r\n").append("\r\n");
        }

        // 替换配置
        config = config.replace("upstream_config_placeholder", upstreamStr.toString());
        config = config.replace("location_config_placeholder", locationStr.toString());
        return config;
    }

}
