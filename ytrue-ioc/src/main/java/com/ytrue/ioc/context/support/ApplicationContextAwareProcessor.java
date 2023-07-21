package com.ytrue.ioc.context.support;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.factory.config.BeanPostProcessor;
import com.ytrue.ioc.context.ApplicationContext;
import com.ytrue.ioc.context.ApplicationContextAware;

/**
 * @author ytrue
 * @date 2022/10/12 10:35
 * @description 由于 ApplicationContext 的获取并不能直接在创建 Bean 时候就可以拿到，所以需要在 refresh 操作时，
 * 把 ApplicationContext 写入到一个包装的 BeanPostProcessor 中去，
 * 再由 AbstractAutowireCapableBeanFactory.applyBeanPostProcessorsBeforeInitialization 方法调用
 */
public class ApplicationContextAwareProcessor implements BeanPostProcessor {

    private final ApplicationContext applicationContext;

    public ApplicationContextAwareProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ApplicationContextAware){
            // 调用setApplicationContext
            ((ApplicationContextAware) bean).setApplicationContext(applicationContext);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
