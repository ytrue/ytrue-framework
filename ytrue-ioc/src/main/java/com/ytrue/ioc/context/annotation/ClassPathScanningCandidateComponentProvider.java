package com.ytrue.ioc.context.annotation;

import cn.hutool.core.util.ClassUtil;
import com.ytrue.ioc.beans.factory.config.BeanDefinition;
import com.ytrue.ioc.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author ytrue
 * @date 2022/10/18 09:49
 * @description 处理对象扫描装配
 * 这里先要提供一个可以通过配置路径 basePackage=com.ytrue.ioc.test.bean，
 * 解析出 classes 信息的工具方法 findCandidateComponents，通过这个方法就可以扫描到所有 @Component 注解的 Bean 对象了
 */
public class ClassPathScanningCandidateComponentProvider {


    /**
     * 查找候选的Components
     *
     * @param basePackage
     * @return
     */
    public Set<BeanDefinition> findCandidateComponents(String basePackage) {
        Set<BeanDefinition> candidates = new LinkedHashSet<>();
        // 扫码指定的包类含有Component注解的class，这里使用的是hutool
        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation(basePackage, Component.class);
        for (Class<?> clazz : classes) {
            candidates.add(new BeanDefinition(clazz));
        }
        return candidates;
    }
}
