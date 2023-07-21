package com.ytrue.ioc.beans.factory.support;

import cn.hutool.core.util.StrUtil;
import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.factory.DisposableBean;
import com.ytrue.ioc.beans.factory.config.BeanDefinition;

import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2022/10/11 15:45
 * @description 销毁方法适配器(接口和配置)
 */
public class DisposableBeanAdapter implements DisposableBean {


    private final static String DESTROY = "destroy";

    /**
     * bean
     */
    private final Object bean;

    /**
     * bean名称
     */
    private final String beanName;

    /**
     * 销毁方法
     */
    private String destroyMethodName;

    public DisposableBeanAdapter(Object bean, String beanName, BeanDefinition beanDefinition) {
        this.bean = bean;
        this.beanName = beanName;
        this.destroyMethodName = beanDefinition.getDestroyMethodName();
    }


    @Override
    public void destroy() throws Exception {
        // 1. 实现接口 DisposableBean
        if (bean instanceof DisposableBean) {
            ((DisposableBean) bean).destroy();
        }

        // 2. 注解配置 destroy-method {判断是为了避免二次执行销毁}
        if ( StrUtil.isNotEmpty(destroyMethodName) && !(bean instanceof DisposableBean && DESTROY.equals(this.destroyMethodName))) {
            Method destroyMethod = bean.getClass().getMethod(destroyMethodName);
            if (null == destroyMethod) {
                throw new BeansException("Couldn't find a destroy method named '" + destroyMethodName + "' on bean with name '" + beanName + "'");
            }
            destroyMethod.invoke(bean);
        }
    }
}
