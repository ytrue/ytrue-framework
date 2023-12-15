package com.ytrue.web.annotation;

import org.springframework.stereotype.Controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ytrue
 * @date 2023-12-15 14:13
 * @description ControllerAdvice
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Controller
public @interface ControllerAdvice {
}
