package com.ytrue.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ytrue
 * @date 2023-12-15 13:48
 * @description RequestHeader
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestHeader {

    // name
    String value() default "";

    // 强制需要
    boolean require() default false;
}
