package com.ytrue.rpc.utils;


import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * @author ytrue
 * @date 2023-05-19 16:39
 * @description ClassLoaderUtils
 */
public class ClassLoaderUtils {
    private static final Set<Class> PRIMITIVESET = new HashSet<>();

    static {
        PRIMITIVESET.add(Integer.class);
        PRIMITIVESET.add(Long.class);
        PRIMITIVESET.add(Float.class);
        PRIMITIVESET.add(Byte.class);
        PRIMITIVESET.add(Short.class);
        PRIMITIVESET.add(Double.class);
        PRIMITIVESET.add(Character.class);
        PRIMITIVESET.add(Boolean.class);
    }

    /**
     * 得到当前ClassLoader
     *
     * @return ClassLoader
     */
    public static ClassLoader getCurrentClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassLoaderUtils.class.getClassLoader();
        }
        return cl == null ? ClassLoader.getSystemClassLoader() : cl;
    }

    /**
     * 得到当前ClassLoader
     *
     * @param clazz 某个类
     * @return ClassLoader
     */
    public static ClassLoader getClassLoader(Class<?> clazz) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) {
            return loader;
        }
        if (clazz != null) {
            loader = clazz.getClassLoader();
            if (loader != null) {
                return loader;
            }
            return clazz.getClassLoader();
        }
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * 根据类名加载Class
     *
     * @param className 类名
     * @return Class
     * @throws ClassNotFoundException 找不到类
     */
    public static Class forName(String className) throws ClassNotFoundException {
        return forName(className, true);
    }

    /**
     * 根据类名加载Class
     *
     * @param className  类名
     * @param initialize 是否初始化
     * @return Class
     * @throws ClassNotFoundException 找不到类
     */
    public static Class forName(String className, boolean initialize) throws ClassNotFoundException {
        return Class.forName(className, initialize, getCurrentClassLoader());
    }

    /**
     * 根据类名加载Class
     *
     * @param className 类名
     * @param cl        Classloader
     * @return Class
     * @throws ClassNotFoundException 找不到类
     */
    public static Class forName(String className, ClassLoader cl) throws ClassNotFoundException {
        return Class.forName(className, true, cl);
    }

    /**
     * 实例化一个对象(只检测默认构造函数，其它不管）
     *
     * @param clazz 对象类
     * @param <T>   对象具体类
     * @return 对象实例
     * @throws Exception 没有找到方法，或者无法处理，或者初始化方法异常等
     */
    public static <T> T newInstance(Class<T> clazz) throws Exception {
        if (PRIMITIVESET.contains(clazz)) {
            return null;
        }
        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            Constructor constructorList[] = clazz.getDeclaredConstructors();
            Constructor defaultConstructor = null;
            for (Constructor con : constructorList) {
                if (con.getParameterTypes().length == 1) {
                    defaultConstructor = con;
                    break;
                }
            }
            if (defaultConstructor != null) {
                if (defaultConstructor.isAccessible()) {
                    return (T) defaultConstructor.newInstance(new Object[]{null});
                } else {
                    try {
                        defaultConstructor.setAccessible(true);
                        return (T) defaultConstructor.newInstance(new Object[]{null});
                    } finally {
                        defaultConstructor.setAccessible(false);
                    }
                }
            } else {
                throw new Exception("The " + clazz.getCanonicalName() + " has no default constructor!");
            }
        }
        try {
            return clazz.newInstance();
        } catch (Exception e) {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            if (constructor.isAccessible()) {
                throw new Exception("The " + clazz.getCanonicalName() + " has no default constructor!", e);
            } else {
                try {
                    constructor.setAccessible(true);
                    return constructor.newInstance();
                } finally {
                    constructor.setAccessible(false);
                }
            }
        }
    }

}
