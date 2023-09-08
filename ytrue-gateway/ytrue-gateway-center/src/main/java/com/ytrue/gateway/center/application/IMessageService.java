package com.ytrue.gateway.center.application;

import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description 消息服务
 */
public interface IMessageService {

    Map<String, String> queryRedisConfig();

    void pushMessage(String gatewayId, Object message);
}
