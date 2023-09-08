package com.ytrue.gateway.center.domain.docker.model.vo;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description 反向代理
 */
public class LocationVO {

    /**
     * 名称
     */
    private String name;
    /**
     * 指向地址
     */
    private String proxy_pass;

    public LocationVO(String name, String proxy_pass) {
        this.name = name;
        this.proxy_pass = proxy_pass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProxy_pass() {
        return proxy_pass;
    }

    public void setProxy_pass(String proxy_pass) {
        this.proxy_pass = proxy_pass;
    }


    @Override
    public String toString() {
        return "LocationVO{" +
               "name='" + name + '\'' +
               ", proxy_pass='" + proxy_pass + '\'' +
               '}';
    }
}
