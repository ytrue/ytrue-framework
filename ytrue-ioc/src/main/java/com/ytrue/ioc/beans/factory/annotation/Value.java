package com.ytrue.ioc.beans.factory.annotation;

import java.lang.annotation.*;

/**
 * @author ytrue
 * @date 2022/10/18 15:19
 * @description Value
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {

    /**
     * The actual value expression: e.g. "#{systemProperties.myProp}".
     *
     * @return
     */
    String value();

}
