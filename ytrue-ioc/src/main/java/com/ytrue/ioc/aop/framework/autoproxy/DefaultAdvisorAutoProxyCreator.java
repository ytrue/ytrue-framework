package com.ytrue.ioc.aop.framework.autoproxy;

import com.ytrue.ioc.aop.*;
import com.ytrue.ioc.aop.aspectj.AspectJExpressionPointcutAdvisor;
import com.ytrue.ioc.aop.framework.ProxyFactory;
import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.PropertyValues;
import com.ytrue.ioc.beans.factory.BeanFactory;
import com.ytrue.ioc.beans.factory.BeanFactoryAware;
import com.ytrue.ioc.beans.factory.config.InstantiationAwareBeanPostProcessor;
import com.ytrue.ioc.beans.factory.support.DefaultListableBeanFactory;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ytrue
 * @date 2022/10/14 10:06
 * @description DefaultAdvisorAutoProxyCreator
 */
public class DefaultAdvisorAutoProxyCreator implements InstantiationAwareBeanPostProcessor, BeanFactoryAware {

    private DefaultListableBeanFactory beanFactory;

    private final Set<Object> earlyProxyReferences = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }


    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        return true;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        return pvs;
    }

    private boolean isInfrastructureClass(Class<?> beanClass) {
        return Advice.class.isAssignableFrom(beanClass)
                || Pointcut.class.isAssignableFrom(beanClass)
                || Advisor.class.isAssignableFrom(beanClass);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!earlyProxyReferences.contains(beanName)) {
            return wrapIfNecessary(bean, beanName);
        }
        return bean;
    }

    protected Object wrapIfNecessary(Object bean, String beanName) {
        // Advice,Pointcut,Advisor是不做处理的
        if (isInfrastructureClass(bean.getClass())) {
            return bean;
        }
        // 获取所有的切面
        Collection<AspectJExpressionPointcutAdvisor> advisors = beanFactory.getBeansOfType(AspectJExpressionPointcutAdvisor.class).values();
        // 循环处理
        for (AspectJExpressionPointcutAdvisor advisor : advisors) {
            ClassFilter classFilter = advisor.getPointcut().getClassFilter();
            // 类是否匹配
            if (!classFilter.matches(bean.getClass())) {
                continue;
            }
            AdvisedSupport advisedSupport = new AdvisedSupport();

            TargetSource targetSource = new TargetSource(bean);
            advisedSupport.setTargetSource(targetSource);
            advisedSupport.setMethodInterceptor((MethodInterceptor) advisor.getAdvice());
            advisedSupport.setMethodMatcher(advisor.getPointcut().getMethodMatcher());
            advisedSupport.setProxyTargetClass(false);

            return new ProxyFactory(advisedSupport).getProxy();
        }
        return bean;
    }

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) {
        earlyProxyReferences.add(beanName);
        return wrapIfNecessary(bean, beanName);
    }

}
