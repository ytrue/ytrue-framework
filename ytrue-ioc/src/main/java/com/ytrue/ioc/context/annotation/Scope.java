package com.ytrue.ioc.context.annotation;

import java.lang.annotation.*;

/**
 * @author ytrue
 * @date 2022/10/18 09:37
 * @description Scope
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {

    String value() default "singleton";
}
