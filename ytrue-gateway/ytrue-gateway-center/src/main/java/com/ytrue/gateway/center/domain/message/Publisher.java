package com.ytrue.gateway.center.domain.message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author ytrue
 * @date 2023-09-08 10:35
 * @description 消息推送
 */
@Service
public class Publisher {

    private final RedisTemplate<String, Object> redisMessageTemplate;

    @Autowired
    public Publisher(RedisTemplate<String, Object> redisMessageTemplate) {
        this.redisMessageTemplate = redisMessageTemplate;
    }

    public void pushMessage(String topic, Object message) {
        redisMessageTemplate.convertAndSend(topic, message);
    }

}
