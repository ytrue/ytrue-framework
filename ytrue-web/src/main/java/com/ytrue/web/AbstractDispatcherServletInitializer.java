package com.ytrue.web;

import com.ytrue.web.context.WebApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ObjectUtils;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

/**
 * @author ytrue
 * @date 2023-12-15 9:49
 * @description 主要核心是onStartUp()中创建ioc和创建DispatcherServlet
 */
public abstract class AbstractDispatcherServletInitializer implements WebApplicationInitializer {

    /**
     * 默认的servlet名称
     */
    public static final String DEFAULT_SERVLET_NAME = "dispatcher";

    /**
     * 默认过滤器名字
     */
    public static final String DEFAULT_FILTER_NAME = "filters";

    /**
     * 1MB
     */
    public static final int M = 1024 * 1024;


    @Override
    public void onStartUp(ServletContext servletContext) {
        // 创建父容器
        final AnnotationConfigApplicationContext rootApplicationContext = createRootApplicationContext();
        // 父容器放入 servletContext
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootApplicationContext);
        // 刷新父容器 -> 在源码当中通过servlet 事件进行refresh
        rootApplicationContext.refresh();


        // 创建子容器
        final WebApplicationContext webApplicationContext = createWebApplicationContext();
        // 创建DispatcherServlet
        final DispatcherServlet dispatcherServlet = new DispatcherServlet(webApplicationContext);
        final ServletRegistration.Dynamic dynamic = servletContext.addServlet(DEFAULT_SERVLET_NAME, dispatcherServlet);

        // 启动加载
        dynamic.setLoadOnStartup(1);
        // 设置文件上传
        final MultipartConfigElement configElement = new MultipartConfigElement(null, 5 * M, 5 * M, 5);
        dynamic.setMultipartConfig(configElement);
        // 设置衍射
        dynamic.addMapping(getMappings());

        // 设置过滤器
        final Filter[] filters = getFilters();
        if (!ObjectUtils.isEmpty(filters)) {
            for (Filter filter : filters) {
                servletContext.addFilter(DEFAULT_FILTER_NAME, filter);
            }
        }
    }

    /**
     * 获取过滤器
     *
     * @return
     */
    protected abstract Filter[] getFilters();

    /**
     * 获取mapping衍射
     *
     * @return
     */
    protected String[] getMappings() {
        return new String[]{"/"};
    }

    /**
     * 创建父容器
     *
     * @return
     */
    protected abstract AnnotationConfigApplicationContext createRootApplicationContext();

    /**
     * 创建子容器
     *
     * @return
     */
    protected abstract WebApplicationContext createWebApplicationContext();

    /**
     * 父容器的配置配置类
     *
     * @return
     */
    protected abstract Class<?>[] getRootConfigClasses();

    /**
     * springmvc配置类
     *
     * @return
     */
    protected abstract Class<?>[] getWebConfigClasses();
}
