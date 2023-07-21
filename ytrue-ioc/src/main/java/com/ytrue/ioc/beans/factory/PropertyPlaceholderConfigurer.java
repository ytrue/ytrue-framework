package com.ytrue.ioc.beans.factory;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.PropertyValue;
import com.ytrue.ioc.beans.PropertyValues;
import com.ytrue.ioc.beans.factory.config.BeanDefinition;
import com.ytrue.ioc.beans.factory.config.BeanFactoryPostProcessor;
import com.ytrue.ioc.core.io.DefaultResourceLoader;
import com.ytrue.ioc.core.io.Resource;
import com.ytrue.ioc.util.StringValueResolver;

import java.io.IOException;
import java.util.Properties;

/**
 * @author ytrue
 * @date 2022/10/18 09:39
 * @description 处理占位符配置
 */
public class PropertyPlaceholderConfigurer implements BeanFactoryPostProcessor {

    /**
     * Default placeholder prefix: {@value}
     */
    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

    /**
     * Default placeholder suffix: {@value}
     */
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

    private String location;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        try {
            // 加载文件
            DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
            Resource resource = resourceLoader.getResource(location);
            Properties properties = new Properties();
            properties.load(resource.getInputStream());

            // 获取所有的beanDefinitionNames
            String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
            // 循环处理
            for (String beanName : beanDefinitionNames) {
                // 获取对应的BeanDefinition
                BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                // 获取对象的属性
                PropertyValues propertyValues = beanDefinition.getPropertyValues();
                // 循环处理
                for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
                    // 获取值
                    Object value = propertyValue.getValue();
                    // 必须是要是string类型
                    if (!(value instanceof String)) {
                        continue;
                    }
                    value = resolvePlaceholder((String) value, properties);
                    propertyValues.addPropertyValue(new PropertyValue(propertyValue.getName(), value));
                    System.out.println(propertyValues);
                }
            }
            // 向容器中添加字符串解析器，供解析@Value注解使用
            StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(properties);
            beanFactory.addEmbeddedValueResolver(valueResolver);
        } catch (IOException e) {
            throw new BeansException("Could not load properties", e);
        }
    }

    private String resolvePlaceholder(String value, Properties properties) {
        String strVal = value;
        StringBuilder buffer = new StringBuilder(strVal);
        int startIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_PREFIX);
        int stopIdx = strVal.indexOf(DEFAULT_PLACEHOLDER_SUFFIX);
        // 如果存在 ${}
        if (startIdx != -1 && stopIdx != -1 && startIdx < stopIdx) {
            String propKey = strVal.substring(startIdx + 2, stopIdx);
            String propVal = properties.getProperty(propKey);
            buffer.replace(startIdx, stopIdx + 1, propVal);
        }
        // 获取properties的内容
        return buffer.toString();
    }


    public void setLocation(String location) {
        this.location = location;
    }


    private class PlaceholderResolvingStringValueResolver implements StringValueResolver {
        private final Properties properties;

        public PlaceholderResolvingStringValueResolver(Properties properties) {
            this.properties = properties;
        }

        @Override
        public String resolveStringValue(String strVal) {
            return PropertyPlaceholderConfigurer.this.resolvePlaceholder(strVal, properties);
        }
    }
}
