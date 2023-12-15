package com.ytrue.web.context;

import org.springframework.beans.factory.Aware;

import javax.servlet.ServletContext;

/**
 * @author ytrue
 * @date 2023-12-15 9:22
 * @description ServletContext 织入
 */
public interface ServletContextAware extends Aware {

    /**
     * 设置servletContext
     *
     * @param servletContext
     */
    void setServletContext(ServletContext servletContext);
}
