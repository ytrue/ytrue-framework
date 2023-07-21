package com.ytrue.ioc.beans.factory.support;

import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.factory.DisposableBean;
import com.ytrue.ioc.beans.factory.ObjectFactory;
import com.ytrue.ioc.beans.factory.config.SingletonBeanRegistry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ytrue
 * @date 2022/9/28 09:19
 * @description DefaultSingletonBeanRegistry
 */
public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {


    /**
     * 空单例对象的内部标记
     * 用作并发映射的标记值（不支持空值）
     */
    protected static final Object NULL_OBJECT = new Object();

    /**
     * 一级缓存，普通对象
     */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

    /**
     * 二级缓存，提前暴漏对象，没有完全实例化的对象
     */
    protected final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>();


    /**
     * 三级缓存，存放代理对象
     */
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>();

    /**
     * 销毁方法的bean
     */
    private final Map<String, DisposableBean> disposableBeans = new LinkedHashMap<>();

    @Override
    public Object getSingleton(String beanName) {
        Object singletonObject = singletonObjects.get(beanName);
        // 如果一级缓存内没有
        if (null == singletonObject) {
            // 去二级缓存拿
            singletonObject = earlySingletonObjects.get(beanName);
            // 判断二级缓存中是否有对象，这个对象就是代理对象，因为只有代理对象才会放到三级缓存中
            if (null == singletonObject) {
                ObjectFactory<?> singletonFactory = singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    // 获取对象
                    singletonObject = singletonFactory.getObject();
                    // 把三级缓存中的代理对象中的真实对象获取出来，放入二级缓存中
                    earlySingletonObjects.put(beanName, singletonObject);
                    singletonFactories.remove(beanName);
                }
            }
        }
        return singletonObject;
    }

    @Override
    public void registerSingleton(String beanName, Object singletonObject) {
        singletonObjects.put(beanName, singletonObject);
        earlySingletonObjects.remove(beanName);
        singletonFactories.remove(beanName);
    }

    /**
     * 添加
     *
     * @param beanName
     * @param singletonFactory
     */
    protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
        // 如果一级缓存不存在这个key
        if (!this.singletonObjects.containsKey(beanName)) {
            this.singletonFactories.put(beanName, singletonFactory);
            this.earlySingletonObjects.remove(beanName);
        }
    }

    public void registerDisposableBean(String beanName, DisposableBean bean) {
        disposableBeans.put(beanName, bean);
    }

    public void destroySingletons() {
        Set<String> keySet = this.disposableBeans.keySet();
        Object[] disposableBeanNames = keySet.toArray();

        for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
            Object beanName = disposableBeanNames[i];
            DisposableBean disposableBean = disposableBeans.remove(beanName);
            try {
                disposableBean.destroy();
            } catch (Exception e) {
                throw new BeansException("Destroy method on bean with name '" + beanName + "' threw an exception", e);
            }
        }
    }
}
