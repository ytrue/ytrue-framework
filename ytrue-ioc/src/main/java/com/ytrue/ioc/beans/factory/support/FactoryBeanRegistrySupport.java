package com.ytrue.ioc.beans.factory.support;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.factory.FactoryBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ytrue
 * @date 2022/10/12 11:11
 * @description <p>
 * FactoryBeanRegistrySupport 类主要处理的就是关于 FactoryBean 此类对象的注册操作，之所以放到这样一个单独的类里，就是希望做到不同领域模块下只负责各自需要完成的功能，避免因为扩展导致类膨胀到难以维护
 * </p>
 * <p>
 * 同样这里也定义了缓存操作 factoryBeanObjectCache，用于存放单例类型的对象，避免重复创建。在日常使用用，基本也都是创建的单例对象
 * </p>
 *
 * <p>
 * doGetObjectFromFactoryBean 是具体的获取 FactoryBean#getObject() 方法，因为既有缓存的处理也有对象的获取，
 * 所以额外提供了 getObjectFromFactoryBean 进行逻辑包装，这部分的操作方式是不和你日常做的业务逻辑开发非常相似。从Redis取数据，如果为空就从数据库获取并写入Redis
 * </p>
 */
public class FactoryBeanRegistrySupport extends DefaultSingletonBeanRegistry {

    /**
     * 由FactoryBeans创建的单例对象缓存：FactoryBean名称-->对象
     */
    private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>();

    /**
     * 获取factoryBeanObjectCache对应的对象
     *
     * @param beanName
     * @return
     */
    protected Object getCachedObjectForFactoryBean(String beanName) {
        Object object = this.factoryBeanObjectCache.get(beanName);
        return (object != NULL_OBJECT ? object : null);
    }

    /**
     * 获取对象
     *
     * @param factory
     * @param beanName
     * @return
     */
    protected Object getObjectFromFactoryBean(FactoryBean factory, String beanName) {
        // 判断是不是单例
        if (factory.isSingleton()) {
            // 判断是否存在，存在直接从map获取不存在调用getObject获取
            Object object = this.factoryBeanObjectCache.get(beanName);
            if (object == null) {
                object = doGetObjectFromFactoryBean(factory, beanName);
                this.factoryBeanObjectCache.put(beanName, (object != null ? object : NULL_OBJECT));
            }
            // 转换一下
            return (object != NULL_OBJECT ? object : null);
        } else {
            return doGetObjectFromFactoryBean(factory, beanName);
        }
    }

    /**
     * 获取FactoryBean创建的对象
     *
     * @param factory
     * @param beanName
     * @return
     */
    private Object doGetObjectFromFactoryBean(FactoryBean factory, String beanName) {
        try {
            return factory.getObject();
        } catch (Exception e) {
            throw new BeansException("FactoryBean threw exception on object[" + beanName + "] creation", e);
        }
    }
}
