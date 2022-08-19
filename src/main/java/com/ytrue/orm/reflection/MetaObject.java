package com.ytrue.orm.reflection;

import com.ytrue.orm.reflection.factory.ObjectFactory;
import com.ytrue.orm.reflection.property.PropertyTokenizer;
import com.ytrue.orm.reflection.wrapper.CollectionWrapper;
import com.ytrue.orm.reflection.wrapper.MapWrapper;
import com.ytrue.orm.reflection.wrapper.ObjectWrapper;
import com.ytrue.orm.reflection.wrapper.ObjectWrapperFactory;
import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author ytrue
 * @date 2022/8/19 14:24
 * @description 元对象
 */
@Getter
public class MetaObject {

    /**
     * 原对象
     */
    private Object originalObject;
    /**
     * 对象包装器
     */

    private ObjectWrapper objectWrapper;
    /**
     * 对象工厂
     */
    private ObjectFactory objectFactory;

    /**
     * 对象包装工厂
     */
    private ObjectWrapperFactory objectWrapperFactory;


    private MetaObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory) {
        this.originalObject = object;
        this.objectFactory = objectFactory;
        this.objectWrapperFactory = objectWrapperFactory;

        if (object instanceof ObjectWrapper) {
            // 如果对象本身已经是ObjectWrapper型，则直接赋给objectWrapper
            this.objectWrapper = (ObjectWrapper) object;
        } else if (objectWrapperFactory.hasWrapperFor(object)) {
            // 如果有包装器,调用ObjectWrapperFactory.getWrapperFor
            this.objectWrapper = objectWrapperFactory.getWrapperFor(this, object);

        } else if (object instanceof Map) {
            // 如果是Map型，返回MapWrapper
            this.objectWrapper = new MapWrapper(this, (Map) object);
        } else if (object instanceof Collection) {
            // 如果是Collection型，返回CollectionWrapper
            this.objectWrapper = new CollectionWrapper(this, (Collection) object);
        } else {
            // 除此以外，返回BeanWrapper
            // this.objectWrapper = new BeanWrapper(this, object);
        }
    }

    public static MetaObject forObject(Object object, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory) {
        if (object == null) {
            // 处理一下null,将null包装起来
            return SystemMetaObject.NULL_META_OBJECT;
        } else {
            return new MetaObject(object, objectFactory, objectWrapperFactory);
        }
    }

    /* --------以下方法都是委派给 ObjectWrapper------ */

    /**
     * 查找属性
     *
     * @param propName
     * @param useCamelCaseMapping
     * @return
     */
    public String findProperty(String propName, boolean useCamelCaseMapping) {
        return objectWrapper.findProperty(propName, useCamelCaseMapping);
    }

    /**
     * 取得getter的名字列表
     *
     * @return
     */
    public String[] getGetterNames() {
        return objectWrapper.getGetterNames();
    }

    /**
     * 取得setter的名字列表
     *
     * @return
     */
    public String[] getSetterNames() {
        return objectWrapper.getSetterNames();
    }

    /**
     * 取得setter的类型列表
     *
     * @param name
     * @return
     */
    public Class<?> getSetterType(String name) {
        return objectWrapper.getSetterType(name);
    }

    /**
     * 取得getter的类型列表
     *
     * @param name
     * @return
     */
    public Class<?> getGetterType(String name) {
        return objectWrapper.getGetterType(name);
    }

    /**
     * 是否有指定的setter
     *
     * @param name
     * @return
     */
    public boolean hasSetter(String name) {
        return objectWrapper.hasSetter(name);
    }

    /**
     * 是否有指定的getter
     *
     * @param name
     * @return
     */
    public boolean hasGetter(String name) {
        return objectWrapper.hasGetter(name);
    }


    /**
     * 是否是集合
     *
     * @return
     */
    public boolean isCollection() {
        return objectWrapper.isCollection();
    }

    /**
     * 添加属性
     *
     * @param element
     */
    public void add(Object element) {
        objectWrapper.add(element);
    }

    /**
     * 添加属性
     *
     * @param list
     * @param <E>
     */
    public <E> void addAll(List<E> list) {
        objectWrapper.addAll(list);
    }

    /**
     * 取得值: 如 班级[0].学生.成绩
     *
     * @param name
     * @return
     */
    public Object getValue(String name) {

        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());
            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                // 如果上层就是null了，那就结束，返回null
                return null;
            } else {
                // 否则继续看下一层，递归调用getValue
                return metaValue.getValue(prop.getChildren());
            }
        } else {
            return objectWrapper.get(prop);
        }
    }

    /**
     * 为属性生成元对象
     *
     * @param name
     * @return
     */
    public MetaObject metaObjectForProperty(String name) {
        Object value = getValue(name);
        return MetaObject.forObject(value, objectFactory, objectWrapperFactory);
    }

    /**
     * 设置值 班级[0].学生.成绩
     *
     * @param name
     * @param value
     */
    public void setValue(String name, Object value) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            MetaObject metaValue = metaObjectForProperty(prop.getIndexedName());

            if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
                if (value == null && prop.getChildren() != null) {
                    // don't instantiate child path if value is null
                    // 如果上层就是 null 了，还得看有没有儿子，没有那就结束
                    return;
                } else {
                    // 否则还得 new 一个，委派给 ObjectWrapper.instantiatePropertyValue
                    metaValue = objectWrapper.instantiatePropertyValue(name, prop, objectFactory);
                }
            }
            // 递归调用setValue
            metaValue.setValue(prop.getChildren(), value);
        } else {
            // 到了最后一层了，所以委派给 ObjectWrapper.set
            objectWrapper.set(prop, value);
        }
    }

}
