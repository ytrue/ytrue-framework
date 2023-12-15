package com.ytrue.web;

import com.ytrue.web.context.AbstractRefreshableWebApplicationContext;
import com.ytrue.web.context.WebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ObjectUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * @author ytrue
 * @date 2023-12-15 9:42
 * @description 初始化ioc容器以及配置信息
 */
public abstract class BaseHttpServlet extends HttpServlet {

    protected ApplicationContext webApplicationContext;

    public BaseHttpServlet(ApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }


    @Override
    public void init() throws ServletException {
        final ServletContext servletContext = getServletContext();

        // 从servletContext 获取父容器
        ApplicationContext rootContext = (ApplicationContext) servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

        // 判断是否创建了webApplicationContext
        if (!ObjectUtils.isEmpty(webApplicationContext)) {
            if (!(this.webApplicationContext instanceof AnnotationConfigApplicationContext)) {
                // 需要转换
                AbstractRefreshableWebApplicationContext wac = (AbstractRefreshableWebApplicationContext) this.webApplicationContext;
                // 设置父子容器
                if (wac.getParent() == null) {
                    wac.setParent(rootContext);
                }
                // 配置上下文
                wac.setServletContext(servletContext);
                wac.setServletConfig(getServletConfig());
                // web容器刷新
                wac.refresh();
            }
        }
        onRefresh(webApplicationContext);
    }


    /**
     * 刷新容器
     *
     * @param webApplicationContext
     */
    protected abstract void onRefresh(ApplicationContext webApplicationContext);
}
