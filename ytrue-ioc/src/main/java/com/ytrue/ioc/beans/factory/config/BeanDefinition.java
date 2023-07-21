package com.ytrue.ioc.beans.factory.config;

import com.ytrue.ioc.beans.PropertyValues;

/**
 * @author ytrue
 * @date 2022/9/27 15:25
 * @description BeanDefinition
 */
public class BeanDefinition {

    /**
     * 单例
     */
    String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;

    /**
     * 多例
     */
    String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;

    /**
     * clazz
     */
    private Class beanClass;

    /**
     * 对象的属性
     */
    private PropertyValues propertyValues;


    /**
     * 初始化方法
     */
    private String initMethodName;

    /**
     * 销毁方法
     */
    private String destroyMethodName;


    /**
     * 默认单例
     */
    private String scope = SCOPE_SINGLETON;

    /**
     * 默认单例
     */
    private boolean singleton = true;

    /**
     * 默认不是多例
     */
    private boolean prototype = false;

    public BeanDefinition(Class beanClass) {
        this.beanClass = beanClass;
        this.propertyValues = new PropertyValues();
    }

    public BeanDefinition(Class beanClass, PropertyValues propertyValues) {
        this.beanClass = beanClass;
        this.propertyValues = propertyValues != null ? propertyValues : new PropertyValues();
    }


    /**
     * 设置作用域
     *
     * @param scope
     */
    public void setScope(String scope) {
        this.scope = scope;
        this.singleton = SCOPE_SINGLETON.equals(scope);
        this.prototype = SCOPE_PROTOTYPE.equals(scope);
    }

    public boolean isSingleton() {
        return singleton;
    }

    public Class getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }

    public PropertyValues getPropertyValues() {
        return propertyValues;
    }

    public void setPropertyValues(PropertyValues propertyValues) {
        this.propertyValues = propertyValues;
    }

    public String getInitMethodName() {
        return initMethodName;
    }

    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    public String getDestroyMethodName() {
        return destroyMethodName;
    }

    public void setDestroyMethodName(String destroyMethodName) {
        this.destroyMethodName = destroyMethodName;
    }

}
