package com.ytrue.web.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * @author ytrue
 * @date 2023-12-15 9:19
 * @description ServletContext后置处理器 主要是对 ServletAware 进行赋值
 */
public class ServletBeanPostProcess implements BeanPostProcessor {

    private ServletContext servletContext;

    private ServletConfig servletConfig;

    public ServletBeanPostProcess(ServletContext servletContext, ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
        this.servletContext = servletContext;
    }


    /**
     * 用户想拿到ServletContext/ServletConfig 需要自行实现XXAware,通过该接口获取属性
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        // 赋值 ServletConfigAware
        if (bean != null && bean instanceof ServletConfigAware) {
            ((ServletConfigAware) bean).setServletConfig(this.servletConfig);
        }

        // 赋值ServletContextAware
        if (bean != null && bean instanceof ServletContextAware) {
            ((ServletContextAware) bean).setServletContext(this.servletContext);
        }
        return bean;
    }

}
