package com.ytrue.gateway.sdk.annotation;

import java.lang.annotation.*;

/**
 * @author ytrue
 * @date 2023-09-09 9:53
 * @description 网关API生产者自定义注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ApiProducerClazz {

    /**
     * 接口名称
     */
    String interfaceName() default "";

    /**
     * 接口版本
     */
    String interfaceVersion() default "";
}
