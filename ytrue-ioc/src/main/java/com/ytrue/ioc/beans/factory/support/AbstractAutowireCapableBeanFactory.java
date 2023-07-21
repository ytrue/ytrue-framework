package com.ytrue.ioc.beans.factory.support;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.TypeUtil;
import com.ytrue.ioc.beans.BeansException;
import com.ytrue.ioc.beans.PropertyValue;
import com.ytrue.ioc.beans.PropertyValues;
import com.ytrue.ioc.beans.factory.*;
import com.ytrue.ioc.beans.factory.config.*;
import com.ytrue.ioc.core.convert.ConversionService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2022/9/28 09:22
 * @description AbstractAutowireCapableBeanFactory
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {


    /**
     * 实例化策略
     */
    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();

    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition, Object[] args) throws BeansException {
        // 判断是否返回代理 Bean 对象
        Object bean = resolveBeforeInstantiation(beanName, beanDefinition);
        if (null != bean) {
            return bean;
        }

        return doCreateBean(beanName, beanDefinition, args);
    }

    protected Object doCreateBean(String beanName, BeanDefinition beanDefinition, Object[] args) {
        Object bean;
        try {
            // 实例化
            bean = createBeanInstance(beanDefinition, beanName, args);

            // 处理循环依赖，将实例化后的Bean对象提前放入缓存中暴露出来
            if (beanDefinition.isSingleton()) {
                Object finalBean = bean;
                addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, beanDefinition, finalBean));

                /*addSingletonFactory(beanName, new ObjectFactory<Object>() {
                    @Override
                    public Object getObject() throws BeansException {
                        return   getEarlyBeanReference(beanName, beanDefinition, finalBean);
                    }
                });*/
            }

            // 实例化后判断
            boolean continueWithPropertyPopulation = applyBeanPostProcessorsAfterInstantiation(beanName, bean);
            if (!continueWithPropertyPopulation) {
                return bean;
            }
            // 在设置 Bean 属性之前，允许 BeanPostProcessor 修改属性值
            applyBeanPostProcessorsBeforeApplyingPropertyValues(beanName, bean, beanDefinition);
            // 给 Bean 填充属性
            applyPropertyValues(beanName, bean, beanDefinition);
            // 执行 Bean 的初始化方法和 BeanPostProcessor 的前置和后置处理方法
            bean = initializeBean(beanName, bean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Instantiation of bean failed", e);
        }

        // 注册实现了 DisposableBean 接口的 Bean 对象
        registerDisposableBeanIfNecessary(beanName, bean, beanDefinition);

        // 判断 SCOPE_SINGLETON、SCOPE_PROTOTYPE
        Object exposedObject = bean;
        if (beanDefinition.isSingleton()) {
            // 获取代理对象
            exposedObject = getSingleton(beanName);
            registerSingleton(beanName, exposedObject);
        }
        return exposedObject;
    }


    protected Object getEarlyBeanReference(String beanName, BeanDefinition beanDefinition, Object bean) {
        Object exposedObject = bean;
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
                exposedObject = ((InstantiationAwareBeanPostProcessor) beanPostProcessor).getEarlyBeanReference(exposedObject, beanName);
                if (null == exposedObject) {
                    return null;
                }
            }
        }

        return exposedObject;
    }

    /**
     * Bean 实例化后对于返回 false 的对象，不在执行后续设置 Bean 对象属性的操作
     *
     * @param beanName
     * @param bean
     * @return
     */
    private boolean applyBeanPostProcessorsAfterInstantiation(String beanName, Object bean) {
        boolean continueWithPropertyPopulation = true;
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor instantiationAwareBeanPostProcessor = (InstantiationAwareBeanPostProcessor) beanPostProcessor;
                if (!instantiationAwareBeanPostProcessor.postProcessAfterInstantiation(bean, beanName)) {
                    continueWithPropertyPopulation = false;
                    break;
                }
            }
        }
        return continueWithPropertyPopulation;
    }


    /**
     * 在设置 Bean 属性之前，允许 BeanPostProcessor 修改属性值
     *
     * @param beanName
     * @param bean
     * @param beanDefinition
     */
    protected void applyBeanPostProcessorsBeforeApplyingPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition) {
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
                PropertyValues pvs = ((InstantiationAwareBeanPostProcessor) beanPostProcessor).postProcessPropertyValues(beanDefinition.getPropertyValues(), bean, beanName);
                if (null != pvs) {
                    for (PropertyValue propertyValue : pvs.getPropertyValues()) {
                        beanDefinition.getPropertyValues().addPropertyValue(propertyValue);
                    }
                }
            }
        }
    }


    protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
        for (BeanPostProcessor beanPostProcessor : getBeanPostProcessors()) {
            if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
                Object result = ((InstantiationAwareBeanPostProcessor) beanPostProcessor).postProcessBeforeInstantiation(beanClass, beanName);
                if (null != result) {
                    return result;
                }
            }
        }
        return null;
    }

    protected Object resolveBeforeInstantiation(String beanName, BeanDefinition beanDefinition) {
        Object bean = applyBeanPostProcessorsBeforeInstantiation(beanDefinition.getBeanClass(), beanName);
        if (null != bean) {
            bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
        }
        return bean;
    }


    /**
     * 注册实现了 DisposableBean 接口的 Bean 对象
     *
     * @param beanName
     * @param bean
     * @param beanDefinition
     */
    protected void registerDisposableBeanIfNecessary(String beanName, Object bean, BeanDefinition beanDefinition) {

        // 非 Singleton 类型的 Bean 不执行销毁方法
        if (!beanDefinition.isSingleton()) {
            return;
        }

        if (bean instanceof DisposableBean || StrUtil.isNotEmpty(beanDefinition.getDestroyMethodName())) {
            registerDisposableBean(beanName, new DisposableBeanAdapter(bean, beanName, beanDefinition));
        }
    }


    /**
     * 初始化
     *
     * @param beanName
     * @param bean
     * @param beanDefinition
     * @throws Exception
     */
    private Object initializeBean(String beanName, Object bean, BeanDefinition beanDefinition) {

        // invokeAwareMethods
        if (bean instanceof Aware) {
            if (bean instanceof BeanFactoryAware) {
                ((BeanFactoryAware) bean).setBeanFactory(this);
            }
            if (bean instanceof BeanClassLoaderAware) {
                ((BeanClassLoaderAware) bean).setBeanClassLoader(getBeanClassLoader());
            }
            if (bean instanceof BeanNameAware) {
                ((BeanNameAware) bean).setBeanName(beanName);
            }
        }


        // 1. 执行 BeanPostProcessor Before 处理
        Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

        // 执行 Bean 对象的初始化方法
        try {
            invokeInitMethods(beanName, wrappedBean, beanDefinition);
        } catch (Exception e) {
            throw new BeansException("Invocation of init method of bean[" + beanName + "] failed", e);
        }
        // 2. 执行 BeanPostProcessor After 处理
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        return wrappedBean;
    }

    /**
     * 初始化方法
     *
     * @param beanName
     * @param bean
     * @param beanDefinition
     */
    private void invokeInitMethods(String beanName, Object bean, BeanDefinition beanDefinition) throws Exception {
        // 1. 实现接口 InitializingBean
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }

        // 2. 注解配置 init-method {判断是为了避免二次执行初始化}
        String initMethodName = beanDefinition.getInitMethodName();
        if (StrUtil.isNotEmpty(initMethodName) && !(bean instanceof InitializingBean)) {
            Method initMethod = beanDefinition.getBeanClass().getMethod(initMethodName);
            if (null == initMethod) {
                throw new BeansException("Could not find an init method named '" + initMethodName + "' on bean with name '" + beanName + "'");
            }
            initMethod.invoke(bean);
        }
    }


    @Override
    public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException {
        Object result = existingBean;
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object current = processor.postProcessBeforeInitialization(result, beanName);
            if (null == current) {
                return result;
            }
            result = current;
        }
        return result;
    }

    @Override
    public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException {
        Object result = existingBean;
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object current = processor.postProcessAfterInitialization(result, beanName);
            if (null == current) {
                return result;
            }
            result = current;
        }
        return result;
    }

    /**
     * Bean 属性填充
     *
     * @param beanName
     * @param bean
     * @param beanDefinition
     */
    protected void applyPropertyValues(String beanName, Object bean, BeanDefinition beanDefinition) {
        try {
            // 获取beanDefinition 的属性
            PropertyValues propertyValues = beanDefinition.getPropertyValues();
            // 循环处理
            for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
                // 获取属性名称
                String name = propertyValue.getName();
                // 获取属性值
                Object value = propertyValue.getValue();
                //
                if (value instanceof BeanReference) {
                    // A 依赖 B，获取 B 的实例化
                    BeanReference beanReference = (BeanReference) value;
                    // 去bean里获取
                    value = getBean(beanReference.getBeanName());
                } else {
                    // 获取值的类型
                    Class<?> sourceType = value.getClass();
                    // 获取对象的类型
                    Class<?> targetType = (Class<?>) TypeUtil.getFieldType(bean.getClass(), name);

                    ConversionService conversionService = getConversionService();
                    if (conversionService != null) {
                        // 是否能转换
                        if (conversionService.canConvert(sourceType, targetType)) {
                            // 转换值
                            value = conversionService.convert(value, targetType);
                        }
                    }
                }
                // 属性填充,这里使用的是hutool的，后面替换成自己q
                BeanUtil.setFieldValue(bean, name, value);
            }
        } catch (Exception e) {
            throw new BeansException("Error setting property values：" + beanName);
        }
    }

    /**
     * 创建实例
     *
     * @param beanDefinition
     * @param beanName
     * @param args
     * @return
     */
    protected Object createBeanInstance(BeanDefinition beanDefinition, String beanName, Object[] args) {
        // 构造函数
        Constructor constructorToUse = null;
        // 获取class
        Class<?> beanClass = beanDefinition.getBeanClass();
        // 获取所有的构造
        Constructor<?>[] declaredConstructors = beanClass.getDeclaredConstructors();
        // 循环处理
        for (Constructor ctor : declaredConstructors) {
            // 找到合适的构造
            if (null != args && ctor.getParameterTypes().length == args.length) {
                constructorToUse = ctor;
                break;
            }
        }
        // 实例化
        return getInstantiationStrategy().instantiate(beanDefinition, beanName, constructorToUse, args);
    }


    /**
     * 获取实例策略
     *
     * @return
     */
    public InstantiationStrategy getInstantiationStrategy() {
        return instantiationStrategy;
    }

    /**
     * 设置实例策略
     *
     * @param instantiationStrategy
     */
    public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }

}
