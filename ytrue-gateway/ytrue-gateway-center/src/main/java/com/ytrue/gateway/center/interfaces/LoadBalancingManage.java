package com.ytrue.gateway.center.interfaces;

import com.ytrue.gateway.center.application.ILoadBalancingService;
import com.ytrue.gateway.center.domain.docker.model.aggregates.NginxConfig;
import com.ytrue.gateway.center.domain.docker.model.vo.LocationVO;
import com.ytrue.gateway.center.domain.docker.model.vo.UpstreamVO;
import com.alibaba.fastjson.JSON;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description https://github.com/docker-java/docker-java
 * https://github.com/docker-java/docker-java/issues/991
 * https://stackoverflow.com/questions/68634253/how-to-create-mysql-dump-of-docker-image-from-java-level
 */
@RestController
@RequestMapping("/wg/admin/load")
public class LoadBalancingManage {

    private final Logger logger = LoggerFactory.getLogger(GatewayConfigManage.class);

    @Resource
    private ILoadBalancingService loadBalancingService;

    /**
     * http://localhost:8001/wg/admin/load/copy
     */
    @GetMapping(value = "copy", produces = "application/json;charset=utf-8")
    public void copy() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("docker", "cp", "/nginx.conf", "D:\\javaCode/ytrue-framework/ytrue-gateway/api-gateway/api-gateway-center/doc/data/nginx/nginx.conf");
        pb.start();
    }

    private static void unTar(TarArchiveInputStream tis, File destFile)
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

    /**
     * http://localhost:8001/wg/admin/load/updateNginxConfig
     */
    @GetMapping(value = "updateNginxConfig", produces = "application/json;charset=utf-8")
    public void updateNginxConfig() {
        List<UpstreamVO> upstreamList = new ArrayList<>();
        upstreamList.add(new UpstreamVO("api01", "least_conn;", Arrays.asList("192.168.1.102:9001;", "192.168.1.102:9002;")));
        upstreamList.add(new UpstreamVO("api02", "least_conn;", Arrays.asList("192.168.1.102:9003;")));

        List<LocationVO> locationList = new ArrayList<>();
        locationList.add(new LocationVO("/api01/", "http://api01;"));
        locationList.add(new LocationVO("/api02/", "http://api02;"));
        NginxConfig nginxConfig = new NginxConfig(upstreamList, locationList);
        try {
            logger.info("刷新Nginx配置文件开始 nginxConfig：{}", JSON.toJSONString(nginxConfig));
            //loadBalancingService.updateNginxConfig(nginxConfig);
            logger.info("刷新Nginx配置文件完成");
        } catch (Exception e) {
            logger.error("刷新Nginx配置文件失败", e);
        }
    }

}
