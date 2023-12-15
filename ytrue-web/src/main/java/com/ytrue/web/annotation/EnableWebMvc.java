package com.ytrue.web.annotation;

import com.ytrue.web.support.DelegatingWebMvcConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ytrue
 * @date 2023-12-15 14:27
 * @description EnableWebMvc
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(DelegatingWebMvcConfiguration.class)
public @interface EnableWebMvc {

}
