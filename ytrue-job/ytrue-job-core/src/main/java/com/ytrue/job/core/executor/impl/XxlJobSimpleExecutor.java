package com.ytrue.job.core.executor.impl;

import com.ytrue.job.core.executor.XxlJobExecutor;
import com.ytrue.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-09-04 11:08
 * @description 不依赖SpringBoot的执行器
 */
public class XxlJobSimpleExecutor extends XxlJobExecutor {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobSimpleExecutor.class);


    private List<Object> xxlJobBeanList = new ArrayList<>();

    public List<Object> getXxlJobBeanList() {
        return xxlJobBeanList;
    }

    public void setXxlJobBeanList(List<Object> xxlJobBeanList) {
        this.xxlJobBeanList = xxlJobBeanList;
    }


    @Override
    public void start() {

        initJobHandlerMethodRepository(xxlJobBeanList);

        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }


    private void initJobHandlerMethodRepository(List<Object> xxlJobBeanList) {
        if (xxlJobBeanList == null || xxlJobBeanList.size() == 0) {
            return;
        }
        for (Object bean : xxlJobBeanList) {
            // method
            Method[] methods = bean.getClass().getDeclaredMethods();
            for (Method executeMethod : methods) {
                XxlJob xxlJob = executeMethod.getAnnotation(XxlJob.class);
                registerJobHandler(xxlJob, bean, executeMethod);
            }
        }
    }
}
