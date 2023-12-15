package com.ytrue.web.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.IOException;

/**
 * @author ytrue
 * @date 2023-12-15 9:17
 * @description 继承AbstractRefreshableConfigApplicationContext，用于容器刷新接入点，插入行为
 */
public abstract class AbstractRefreshableWebApplicationContext extends AbstractRefreshableConfigApplicationContext implements ConfigurableWebApplicationContext {


    protected ServletConfig servletConfig;

    protected ServletContext servletContext;

    @Override
    public void setServletConfig(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }


    /**
     * 加载bf
     *
     * @param beanFactory the bean factory to load bean definitions into
     * @throws BeansException
     * @throws IOException
     */
    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
        // 注册ServletContext后置处理器
        beanFactory.addBeanPostProcessor(new ServletBeanPostProcess(this.servletContext, this.servletConfig));

        // A类中有test方法，B类实现A类，且有一个test属性。也就是说当前会有一个set方法和一个set注入。
        // 在特定场景下会干扰。因此忽略set注入,这里是交给ServletBeanPostProcess去处理的
        beanFactory.ignoreDependencyInterface(ServletContextAware.class);
        beanFactory.ignoreDependencyInterface(ServletConfigAware.class);
    }
}
