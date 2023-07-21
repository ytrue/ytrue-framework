package com.ytrue.middleware.orm.spring;

import com.ytrue.orm.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;

import java.beans.Introspector;
import java.io.IOException;

/**
 * @author ytrue
 * @date 2022/9/10 12:14
 * @description MapperScannerConfigurer
 */
public class MapperScannerConfigurer implements BeanDefinitionRegistryPostProcessor {

    private String basePackage;
    private SqlSessionFactory sqlSessionFactory;


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        try {
            // classpath*:com.ytrue/**/dao/**/*.class
            String packageSearchPath = "classpath*:" + basePackage.replace('.', '/') + "/**/*.class";

            // 找到匹配的class文件路径
            ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);

            // 处理 mapper的class，注册 beanDefinition
            for (Resource resource : resources) {
                //
                MetadataReader metadataReader = new SimpleMetadataReader(resource, ClassUtils.getDefaultClassLoader());

                // 转成 beanDefinition
                ScannedGenericBeanDefinition beanDefinition = new ScannedGenericBeanDefinition(metadataReader);
                String beanName = Introspector.decapitalize(ClassUtils.getShortName(beanDefinition.getBeanClassName()));
                beanDefinition.setResource(resource);
                beanDefinition.setSource(resource);
                // 单例
                beanDefinition.setScope("singleton");

                // 这里就是将这个class传递到有参构造方法
                beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition.getBeanClassName());
                beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(sqlSessionFactory);
                // 将所有的mapper接口改为MapperFactoryBean类
                beanDefinition.setBeanClass(MapperFactoryBean.class);

                // 注册 beanDefinition
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
                registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
}
