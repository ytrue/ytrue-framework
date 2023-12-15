package com.ytrue.web.context;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * @author ytrue
 * @date 2023-12-15 9:15
 * @description 用于制定存放ServletContext和ServletConfig规范
 * ServletContext和ServletConfig是Servlet中的上下文以及配置类，因此我们需要将其保存下来
 */
public interface ConfigurableWebApplicationContext extends WebApplicationContext {

    /**
     * 设置 servletContext
     *
     * @param servletContext
     */
    void setServletContext(ServletContext servletContext);

    /**
     * 设置 servletConfig
     *
     * @param servletConfig
     */
    void setServletConfig(ServletConfig servletConfig);

    /**
     * 获取getServletContext
     *
     * @return
     */
    ServletContext getServletContext();

    /**
     * getServletConfig
     *
     * @return
     */
    ServletConfig getServletConfig();
}
