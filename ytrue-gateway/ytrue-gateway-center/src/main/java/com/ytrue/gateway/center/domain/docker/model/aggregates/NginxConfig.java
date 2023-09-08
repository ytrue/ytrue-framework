package com.ytrue.gateway.center.domain.docker.model.aggregates;


import com.ytrue.gateway.center.domain.docker.model.vo.LocationVO;
import com.ytrue.gateway.center.domain.docker.model.vo.UpstreamVO;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description Nginx 负载配置信息
 */
public class NginxConfig {

    private String applicationName;
    private String nginxName;
    private String localNginxPath;
    private String remoteNginxPath;
    /**
     * 负载均衡列表
     */
    private List<UpstreamVO> upstreamList;
    /**
     * 反向代理列表
     */
    private List<LocationVO> locationList;

    public NginxConfig(String applicationName, String nginxName, String localNginxPath, String remoteNginxPath, List<UpstreamVO> upstreamList, List<LocationVO> locationList) {
        this.applicationName = applicationName;
        this.nginxName = nginxName;
        this.localNginxPath = localNginxPath;
        this.remoteNginxPath = remoteNginxPath;
        this.upstreamList = upstreamList;
        this.locationList = locationList;
    }

    public NginxConfig(List<UpstreamVO> upstreamList, List<LocationVO> locationList) {
        this.applicationName = "api-gateway-center";
        this.nginxName = "Nginx";
        this.localNginxPath = "/Users/fuzhengwei/1024/KnowledgePlanet/api-gateway/api-gateway-center/doc/data/nginx/nginx.conf";
        this.remoteNginxPath = "/etc/nginx/";
        this.upstreamList = upstreamList;
        this.locationList = locationList;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getNginxName() {
        return nginxName;
    }

    public void setNginxName(String nginxName) {
        this.nginxName = nginxName;
    }

    public String getLocalNginxPath() {
        return localNginxPath;
    }

    public void setLocalNginxPath(String localNginxPath) {
        this.localNginxPath = localNginxPath;
    }

    public String getRemoteNginxPath() {
        return remoteNginxPath;
    }

    public void setRemoteNginxPath(String remoteNginxPath) {
        this.remoteNginxPath = remoteNginxPath;
    }

    public List<UpstreamVO> getUpstreamList() {
        return upstreamList;
    }

    public void setUpstreamList(List<UpstreamVO> upstreamList) {
        this.upstreamList = upstreamList;
    }

    public List<LocationVO> getLocationList() {
        return locationList;
    }

    public void setLocationList(List<LocationVO> locationList) {
        this.locationList = locationList;
    }

}
