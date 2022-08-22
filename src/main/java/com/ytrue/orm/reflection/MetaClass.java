package com.ytrue.orm.reflection;

import com.ytrue.orm.reflection.invoker.GetFieldInvoker;
import com.ytrue.orm.reflection.invoker.Invoker;
import com.ytrue.orm.reflection.invoker.MethodInvoker;
import com.ytrue.orm.reflection.property.PropertyTokenizer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author ytrue
 * @date 2022/8/22 11:00
 * @description MetaClass 其实这个类基于是对Reflector的方法封装
 */
public class MetaClass {

    private Reflector reflector;

    /**
     * 构建class 的 Reflector
     *
     * @param type
     */
    private MetaClass(Class<?> type) {
        this.reflector = Reflector.forClass(type);
    }

    /**
     * 调用构造
     *
     * @param type
     * @return
     */
    public static MetaClass forClass(Class<?> type) {
        return new MetaClass(type);
    }

    /**
     * Reflector 是否开启了缓存
     *
     * @return
     */
    public static boolean isClassCacheEnabled() {
        return Reflector.isClassCacheEnabled();
    }

    /**
     * 设置 Reflector 的缓存
     *
     * @param classCacheEnabled
     */
    public static void setClassCacheEnabled(boolean classCacheEnabled) {
        Reflector.setClassCacheEnabled(classCacheEnabled);
    }


    /**
     * 根据 name 获取 Reflector 的  get 类型列表指定数据再构建 Reflector
     *
     * @param name
     * @return
     */
    public MetaClass metaClassForProperty(String name) {
        Class<?> propType = reflector.getGetterType(name);
        return MetaClass.forClass(propType);
    }


    public String findProperty(String name) {
        StringBuilder prop = buildProperty(name, new StringBuilder());
        return prop.length() > 0 ? prop.toString() : null;
    }

    public String findProperty(String name, boolean useCamelCaseMapping) {
        if (useCamelCaseMapping) {
            name = name.replace("_", "");
        }
        return findProperty(name);
    }

    /**
     * list[0].id = 会变成 list.  如果是 data.id 就是 data.id
     *
     * @param name
     * @param builder
     * @return
     */
    private StringBuilder buildProperty(String name, StringBuilder builder) {

        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            String propertyName = reflector.findPropertyName(prop.getName());
            if (propertyName != null) {
                builder.append(propertyName);
                builder.append(".");
                MetaClass metaProp = metaClassForProperty(propertyName);
                metaProp.buildProperty(prop.getChildren(), builder);
            }
        } else {
            String propertyName = reflector.findPropertyName(name);
            if (propertyName != null) {
                builder.append(propertyName);
            }
        }
        return builder;
    }

    /**
     * 获取 Reflector 的 get 属性列表
     *
     * @return
     */
    public String[] getGetterNames() {
        return reflector.getGetablePropertyNames();
    }

    /**
     * 设置 Reflector 的 set 属性列表
     *
     * @return
     */
    public String[] getSetterNames() {
        return reflector.getSetablePropertyNames();
    }


    /**
     * 获取 list[0].id 这里 id 属性的类型
     *
     * @param name
     * @return
     */
    public Class<?> getSetterType(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            // 获取到指定的
            MetaClass metaProp = metaClassForProperty(prop.getName());
            return metaProp.getSetterType(prop.getChildren());
        } else {
            return reflector.getSetterType(prop.getName());
        }
    }

    /**
     * 获取 list[0].id 这里 id 属性的类型
     *
     * @param name
     * @return
     */
    public Class<?> getGetterType(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            MetaClass metaProp = metaClassForProperty(prop);
            return metaProp.getGetterType(prop.getChildren());
        }
        // issue #506. Resolve the type inside a Collection Object
        return getGetterType(prop);
    }


    /**
     * data.id  获取  data 的类型
     *
     * @param prop
     * @return
     */
    private Class<?> getGetterType(PropertyTokenizer prop) {
        Class<?> type = reflector.getGetterType(prop.getName());

        if (prop.getIndex() != null && Collection.class.isAssignableFrom(type)) {
            Type returnType = getGenericGetterType(prop.getName());

            if (returnType instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) returnType).getActualTypeArguments();
                if (actualTypeArguments != null && actualTypeArguments.length == 1) {
                    returnType = actualTypeArguments[0];
                    if (returnType instanceof Class) {
                        type = (Class<?>) returnType;
                    } else if (returnType instanceof ParameterizedType) {
                        type = (Class<?>) ((ParameterizedType) returnType).getRawType();
                    }
                }
            }
        }
        return type;
    }

    /**
     * 构建 MetaClass
     *
     * @param prop
     * @return
     */
    private MetaClass metaClassForProperty(PropertyTokenizer prop) {
        Class<?> propType = getGetterType(prop);
        return MetaClass.forClass(propType);
    }

    /**
     * data.id  获取 id 的返回类型，如果是id字段是返回字段的类型，如果是方法是返回方法的类型
     *
     * @param propertyName
     * @return
     */
    private Type getGenericGetterType(String propertyName) {
        try {
            Invoker invoker = reflector.getGetInvoker(propertyName);
            if (invoker instanceof MethodInvoker) {
                Field _method = MethodInvoker.class.getDeclaredField("method");
                _method.setAccessible(true);
                Method method = (Method) _method.get(invoker);
                return method.getGenericReturnType();
            } else if (invoker instanceof GetFieldInvoker) {
                Field _field = GetFieldInvoker.class.getDeclaredField("field");
                _field.setAccessible(true);
                Field field = (Field) _field.get(invoker);
                return field.getGenericType();
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return null;
    }

    /**
     * data.id 判断 id 是否存在set方法
     *
     * @param name
     * @return
     */
    public boolean hasSetter(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            if (reflector.hasSetter(prop.getName())) {
                MetaClass metaProp = metaClassForProperty(prop.getName());
                return metaProp.hasSetter(prop.getChildren());
            } else {
                return false;
            }
        } else {
            return reflector.hasSetter(prop.getName());
        }
    }

    /**
     * data.id 判断 id 是否存在get方法
     *
     * @param name
     * @return
     */
    public boolean hasGetter(String name) {
        PropertyTokenizer prop = new PropertyTokenizer(name);
        if (prop.hasNext()) {
            if (reflector.hasGetter(prop.getName())) {
                MetaClass metaProp = metaClassForProperty(prop);
                return metaProp.hasGetter(prop.getChildren());
            } else {
                return false;
            }
        } else {
            return reflector.hasGetter(prop.getName());
        }
    }

    public Invoker getGetInvoker(String name) {
        return reflector.getGetInvoker(name);
    }

    public Invoker getSetInvoker(String name) {
        return reflector.getSetInvoker(name);
    }


    /**
     * 是否有默认构造
     *
     * @return
     */
    public boolean hasDefaultConstructor() {
        return reflector.hasDefaultConstructor();
    }
}
