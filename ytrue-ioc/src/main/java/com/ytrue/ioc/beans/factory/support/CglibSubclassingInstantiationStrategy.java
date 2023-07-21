package com.ytrue.ioc.beans.factory.support;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.factory.config.BeanDefinition;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;

import java.lang.reflect.Constructor;

/**
 * @author ytrue
 * @date 2022/9/28 16:36
 * @description CglibSubclassingInstantiationStrategy
 */
public class CglibSubclassingInstantiationStrategy implements InstantiationStrategy {

    @Override
    public Object instantiate(BeanDefinition beanDefinition, String beanName, Constructor ctor, Object[] args) throws BeansException {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(beanDefinition.getBeanClass());

        enhancer.setCallback(new NoOp() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
        });

        if (null == ctor) {
            return enhancer.create();
        }
        return enhancer.create(ctor.getParameterTypes(), args);
    }

}
