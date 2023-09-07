package com.ytrue.gateway.core.authorization;

/**
 * @author ytrue
 * @date 2023-09-07 11:09
 * @description 认证服务接口
 */
public interface IAuth {

    /**
     * 验证
     *
     * @param id
     * @param token
     * @return
     */
    boolean validate(String id, String token);
}
