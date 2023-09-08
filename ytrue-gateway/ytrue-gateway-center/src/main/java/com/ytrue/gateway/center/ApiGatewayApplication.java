package com.ytrue.gateway.center;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author ytrue
 * @date 2023-09-08 10:35
 * @description ApiGatewayApplication
 *
 * @Configurable 平时用的却较少，它用于解决非Spring容器管理的Bean中却依赖Spring Bean的场景，也就是说Bean A依赖了一个Spring的Bean B，但是A不是Spring 得Bean所以无法进行属性注入拿不到B的情况
 */
@SpringBootApplication
@Configurable
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
