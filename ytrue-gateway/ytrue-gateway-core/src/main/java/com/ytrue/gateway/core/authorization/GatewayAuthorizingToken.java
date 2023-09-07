package com.ytrue.gateway.core.authorization;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * @author ytrue
 * @date 2023-09-07 11:10
 * @description 验证 Token
 */
public class GatewayAuthorizingToken implements AuthenticationToken {

    private static final long serialVersionUID = 1L;

    /**
     * 通信管道ID
     */
    private String channelId;

    /**
     * JSON WEB TOKEN
     */
    private String jwt;

    public GatewayAuthorizingToken() {
    }

    public GatewayAuthorizingToken(String channelId, String jwt) {
        this.channelId = channelId;
        this.jwt = jwt;
    }

    @Override
    public Object getPrincipal() {
        return channelId;
    }

    @Override
    public Object getCredentials() {
        return this.jwt;
    }


    // --------------------------------------- get and set
    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

}
