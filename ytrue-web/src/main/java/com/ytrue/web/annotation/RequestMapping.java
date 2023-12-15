package com.ytrue.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ytrue
 * @date 2023-12-15 10:36
 * @description RequestMapping
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {


    /**
     * 具体路径
     *
     * @return
     */
    String value() default "";

    /**
     * 具体请求
     *
     * @return
     */
    RequestMethod[] requestMethod() default {};
}
