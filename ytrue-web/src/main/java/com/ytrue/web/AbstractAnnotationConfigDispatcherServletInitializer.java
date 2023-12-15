package com.ytrue.web;

import com.ytrue.web.context.AnnotationConfigWebApplicationContext;
import com.ytrue.web.context.WebApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ObjectUtils;

import javax.servlet.Filter;

/**
 * @author ytrue
 * @date 2023-12-15 9:49
 * @description 作用是根据留给子类的配置类创建spring ioc以及web ioc
 */
public abstract class AbstractAnnotationConfigDispatcherServletInitializer extends AbstractDispatcherServletInitializer{

    @Override
    protected AnnotationConfigApplicationContext createRootApplicationContext() {

        final Class<?>[] rootConfigClasses = getRootConfigClasses();
        if (!ObjectUtils.isEmpty(rootConfigClasses)){
            final AnnotationConfigApplicationContext rootContext = new AnnotationConfigApplicationContext();
            rootContext.register(rootConfigClasses);
            return rootContext;
        }
        return null;
    }


    @Override
    protected WebApplicationContext createWebApplicationContext() {
        final Class<?>[] webConfigClasses = getWebConfigClasses();
        if (!ObjectUtils.isEmpty(webConfigClasses)){
            final AnnotationConfigWebApplicationContext webContext = new AnnotationConfigWebApplicationContext();
            webContext.register(webConfigClasses);
            return webContext;
        }
        return null;
    }

    @Override
    protected Filter[] getFilters() {
        return new Filter[0];
    }
}
