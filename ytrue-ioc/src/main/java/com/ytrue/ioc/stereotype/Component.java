package com.ytrue.ioc.stereotype;

import java.lang.annotation.*;

/**
 * @author ytrue
 * @date 2022/10/18 09:36
 * @description Component
 * Indicates that an annotated class is a "component".
 * Such classes are considered as candidates for auto-detection
 * when using annotation-based configuration and classpath scanning.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Component {

    String value() default "";
}
