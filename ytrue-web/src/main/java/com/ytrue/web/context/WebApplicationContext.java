package com.ytrue.web.context;

import org.springframework.context.ApplicationContext;

/**
 * @author ytrue
 * @date 2023-12-15 9:14
 * @description web ioc根接口
 */
public interface WebApplicationContext extends ApplicationContext {


    /**
     * 用于设置和获取根 WebApplicationContext 对象的属性。根 WebApplicationContext 是整个 Web 应用程序的根容器，
     * 负责管理整个应用程序的 Bean 实例。通过设置该属性，
     * 可以将根 WebApplicationContext 对象绑定到 ServletContext 中，以便在整个应用程序中共享和访问。
     */
    String ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".ROOT";

    /**
     * 用于设置和获取子 WebApplicationContext 对象的属性。子 WebApplicationContext 是根 WebApplicationContext 的子容器，
     * 用于管理特定范围的 Bean 实例。通过设置该属性，
     * 可以将子 WebApplicationContext 对象绑定到 ServletRequest 或 HttpSession 中，以便在特定范围内共享和访问。
     */
    String CHILD_WEB_APPLICATION_CONTEXT_ATTRIBUTE = WebApplicationContext.class.getName() + ".CHILD";
}
