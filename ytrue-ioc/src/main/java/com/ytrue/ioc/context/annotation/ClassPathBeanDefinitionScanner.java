package com.ytrue.ioc.context.annotation;

import cn.hutool.core.util.StrUtil;
import com.ytrue.ioc.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import com.ytrue.ioc.beans.factory.config.BeanDefinition;
import com.ytrue.ioc.beans.factory.support.BeanDefinitionRegistry;
import com.ytrue.ioc.stereotype.Component;

import java.util.Set;

/**
 * @author ytrue
 * @date 2022/10/18 09:50
 * @description ClassPathBeanDefinitionScanner 是继承自 ClassPathScanningCandidateComponentProvider 的具体扫描包处理的类，
 * 在 doScan 中除了获取到扫描的类信息以后，还需要获取 Bean 的作用域和类名，如果不配置类名基本都是把首字母缩写
 */
public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {

    private BeanDefinitionRegistry registry;

    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        this.registry = registry;
    }

    public void doScan(String... basePackages) {
        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
            for (BeanDefinition beanDefinition : candidates) {
                // 解析 Bean 的作用域 singleton、prototype
                String beanScope = resolveBeanScope(beanDefinition);
                if (StrUtil.isNotEmpty(beanScope)) {
                    beanDefinition.setScope(beanScope);
                }
                registry.registerBeanDefinition(determineBeanName(beanDefinition), beanDefinition);
            }
        }

        // 注册处理注解的 BeanPostProcessor（@Autowired、@Value）
        registry.registerBeanDefinition("com.ytrue.ioc.beans.factory.annotation.internalAutowiredAnnotationProcessor",
                new BeanDefinition(AutowiredAnnotationBeanPostProcessor.class));

    }

    private String resolveBeanScope(BeanDefinition beanDefinition) {
        Class<?> beanClass = beanDefinition.getBeanClass();
        Scope scope = beanClass.getAnnotation(Scope.class);
        if (null != scope) {
            return scope.value();
        }
        return StrUtil.EMPTY;
    }

    private String determineBeanName(BeanDefinition beanDefinition) {
        Class<?> beanClass = beanDefinition.getBeanClass();
        Component component = beanClass.getAnnotation(Component.class);
        String value = component.value();
        if (StrUtil.isEmpty(value)) {
            value = StrUtil.lowerFirst(beanClass.getSimpleName());
        }
        return value;
    }

}
