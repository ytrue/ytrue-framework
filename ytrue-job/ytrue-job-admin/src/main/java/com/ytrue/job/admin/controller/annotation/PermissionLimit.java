package com.ytrue.job.admin.controller.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ytrue
 * @date 2023-08-29 9:30
 * @description 权限注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionLimit {

    //登录拦截，默认为true
    boolean limit() default true;

    //管理员权限，默认为false
    boolean adminuser() default false;

}
