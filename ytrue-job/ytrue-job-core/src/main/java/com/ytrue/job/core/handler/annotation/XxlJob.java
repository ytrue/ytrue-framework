package com.ytrue.job.core.handler.annotation;

import java.lang.annotation.*;

/**
 * @author ytrue
 * @date 2023-08-28 11:33
 * @description XxlJob
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlJob {

    /**
     * 定时任务的名称
     *
     * @return
     */
    String value();

    /**
     * 初始化方法
     *
     * @return
     */
    String init() default "";

    /**
     * 销毁方法
     *
     * @return
     */
    String destroy() default "";

}
