package com.ytrue.web.context;

import org.springframework.beans.factory.Aware;

import javax.servlet.ServletConfig;

/**
 * @author ytrue
 * @date 2023-12-15 9:23
 * @description ServletConfig 织入
 */
public interface ServletConfigAware extends Aware {

    /**
     * 设置ServletConfig
     * @param servletConfig
     */
    void setServletConfig(ServletConfig servletConfig);
}
