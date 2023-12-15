package com.ytrue.web;

import javax.servlet.ServletContext;

/**
 * @author ytrue
 * @date 2023-12-15 9:47
 * @description WebApplicationInitializer 接口的作用是用于配置和初始化 Web 应用程序的
 */
public interface WebApplicationInitializer {

    /**
     * onStartup 方法用于在应用程序启动时进行配置和初始化操作。
     * @param servletContext
     */
    void onStartUp(ServletContext servletContext);
}
