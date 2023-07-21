package com.ytrue.ioc.beans.factory.annotation;

import java.lang.annotation.*;

/**
 * @author ytrue
 * @date 2022/10/18 15:19
 * @description Qualifier
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Qualifier {
    String value() default "";
}
