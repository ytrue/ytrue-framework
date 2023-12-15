package com.ytrue.web.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ytrue
 * @date 2023-12-15 10:35
 * @description PostMapping
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping(requestMethod = RequestMethod.POST)
public @interface PostMapping {

    @AliasFor(annotation = RequestMapping.class)
    String value() default "";
}
