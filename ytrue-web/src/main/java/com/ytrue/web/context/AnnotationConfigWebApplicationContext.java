package com.ytrue.web.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author ytrue
 * @date 2023-12-15 9:24
 * @description t web ioc实现：注解版
 */
public class AnnotationConfigWebApplicationContext extends AbstractRefreshableWebApplicationContext implements AnnotationConfigRegistry {


    /**
     * bean name 生成器
     */
    private BeanNameGenerator beanNameGenerator;


    /**
     * 作用范围解析
     */
    private ScopeMetadataResolver scopeMetadataResolver;

    /**
     * 配置bean
     */
    private final Set<Class<?>> componentClasses = new LinkedHashSet<>();

    /**
     * 扫描的包
     */
    private final Set<String> basePackages = new LinkedHashSet<>();


    /**
     * 加载bf
     *
     * @param beanFactory the bean factory to load bean definitions into
     * @throws BeansException
     * @throws IOException
     */
    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {

        // 解析注解bean
        AnnotatedBeanDefinitionReader reader = getAnnotatedBeanDefinitionReader(beanFactory);
        // 解析class path bean
        ClassPathBeanDefinitionScanner scanner = getClassPathBeanDefinitionScanner(beanFactory);


        // 获取bean name 生成器
        BeanNameGenerator beanNameGenerator = getBeanNameGenerator();
        if (beanNameGenerator != null) {
            // 设置bean name 生成器
            reader.setBeanNameGenerator(beanNameGenerator);
            // 设置bean name 生成器
            scanner.setBeanNameGenerator(beanNameGenerator);
            // 注册bean org.springframework.context.annotation.internalConfigurationBeanNameGenerator
            beanFactory.registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
        }

        ScopeMetadataResolver scopeMetadataResolver = getScopeMetadataResolver();
        // 设置 作用范围解析
        if (scopeMetadataResolver != null) {
            reader.setScopeMetadataResolver(scopeMetadataResolver);
            scanner.setScopeMetadataResolver(scopeMetadataResolver);
        }

        // 如果componentClasses 不为空
        if (!this.componentClasses.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering component classes: [" + StringUtils.collectionToCommaDelimitedString(this.componentClasses) + "]");
            }

            // 就把这个componentClasses 注册bean
            reader.register(ClassUtils.toClassArray(this.componentClasses));
        }

        // 和上面同理
        if (!this.basePackages.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Scanning base packages: [" + StringUtils.collectionToCommaDelimitedString(this.basePackages) + "]");
            }
            scanner.scan(StringUtils.toStringArray(this.basePackages));
        }


        // 这是是处理配置文件的，这个可以不关注
        String[] configLocations = getConfigLocations();
        if (configLocations != null) {
            for (String configLocation : configLocations) {
                try {
                    Class<?> clazz = ClassUtils.forName(configLocation, getClassLoader());
                    if (logger.isTraceEnabled()) {
                        logger.trace("Registering [" + configLocation + "]");
                    }
                    reader.register(clazz);
                } catch (ClassNotFoundException ex) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Could not load class for config location [" + configLocation + "] - trying package scan. " + ex);
                    }
                    int count = scanner.scan(configLocation);
                    if (count == 0 && logger.isDebugEnabled()) {
                        logger.debug("No component classes found for specified class/package [" + configLocation + "]");
                    }
                }
            }
        }

    }

    /**
     * 读取和解析带有注解的 Bean 定义的对象
     *
     * @param beanFactory
     * @return
     */
    protected AnnotatedBeanDefinitionReader getAnnotatedBeanDefinitionReader(DefaultListableBeanFactory beanFactory) {
        return new AnnotatedBeanDefinitionReader(beanFactory, getEnvironment());
    }


    /**
     * 根据class path解析
     *
     * @param beanFactory
     * @return
     */
    protected ClassPathBeanDefinitionScanner getClassPathBeanDefinitionScanner(DefaultListableBeanFactory beanFactory) {
        return new ClassPathBeanDefinitionScanner(beanFactory, true, getEnvironment());
    }


    /**
     * 添加componentClasses
     *
     * @param componentClasses one or more component classes,
     *                         e.g. {@link Configuration @Configuration} classes
     */
    @Override
    public void register(Class<?>... componentClasses) {
        Assert.notEmpty(componentClasses, "At least one component class must be specified");
        Collections.addAll(this.componentClasses, componentClasses);
    }

    /**
     * 添加basePackages
     *
     * @param basePackages the packages to scan for component classes
     */
    @Override
    public void scan(String... basePackages) {
        Assert.notEmpty(basePackages, "At least one base package must be specified");
        Collections.addAll(this.basePackages, basePackages);
    }


    protected BeanNameGenerator getBeanNameGenerator() {
        return this.beanNameGenerator;
    }

    protected ScopeMetadataResolver getScopeMetadataResolver() {
        return this.scopeMetadataResolver;
    }
}
