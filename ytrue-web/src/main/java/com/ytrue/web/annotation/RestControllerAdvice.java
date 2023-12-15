package com.ytrue.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ytrue
 * @date 2023-12-15 14:13
 * @description RestControllerAdvice
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ResponseBody
@ControllerAdvice
public @interface RestControllerAdvice {
}
