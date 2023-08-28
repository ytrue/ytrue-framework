package com.ytrue.job.core.glue;

import com.ytrue.job.core.glue.impl.SpringGlueFactory;
import com.ytrue.job.core.handler.IJobHandler;
import groovy.lang.GroovyClassLoader;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author ytrue
 * @date 2023-08-28 11:39
 * @description GlueFactory
 */
public class GlueFactory {

    /**
     * 实例
     */
    private static GlueFactory glueFactory = new GlueFactory();


    /**
     * 获取实例
     *
     * @return
     */
    public static GlueFactory getInstance() {
        return glueFactory;
    }

    /**
     * 重新创建一个实例，并且赋值
     *
     * @param type
     */
    public static void refreshInstance(int type) {
        if (type == 0) {
            glueFactory = new GlueFactory();
        } else if (type == 1) {
            glueFactory = new SpringGlueFactory();
        }
    }

    /**
     * 用 Groovy 的 GroovyClassLoader ，动态地加载一个脚本并执行它的行为。GroovyClassLoader是一个定制的类装载器，负责解释加载Java类中用到的Groovy类。
     */
    private GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

    /**
     * 缓存
     */
    private ConcurrentMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();


    /**
     * 在该方法中创建IJobHandler对象
     *
     * @param codeSource
     * @return
     * @throws Exception
     */
    public IJobHandler loadNewInstance(String codeSource) throws Exception {
        //对用户在线编辑的源码做判空校验
        if (codeSource != null && codeSource.trim().length() > 0) {
            //把源码转化为Class文件
            Class<?> clazz = getCodeSourceClass(codeSource);
            if (clazz != null) {
                //创建对象
                Object instance = clazz.newInstance();
                if (instance != null) {
                    //上面是我从xxl-job复制过来的默认例子，可以看到，在新编写的类都要继承IJobHandler抽象类的
                    //所以这里要判断一下是否属于这个对象
                    if (instance instanceof IJobHandler) {
                        //这里其实做的就是属性注入的工作
                        this.injectService(instance);
                        return (IJobHandler) instance;
                    } else {
                        throw new IllegalArgumentException(">>>>>>>>>>> xxl-glue, loadNewInstance error, "
                                                           + "cannot convert from instance[" + instance.getClass() + "] to IJobHandler");
                    }
                }
            }
        }
        throw new IllegalArgumentException(">>>>>>>>>>> xxl-glue, loadNewInstance error, instance is null");
    }


    private Class<?> getCodeSourceClass(String codeSource) {
        try {
            //可以看到，这里其实是用MD5把源码加密成字节
            byte[] md5 = MessageDigest.getInstance("MD5").digest(codeSource.getBytes());
            String md5Str = new BigInteger(1, md5).toString(16);
            //从对应的缓存中查看是否已经缓存了该字节了，如果有就可以直接返回class文件
            Class<?> clazz = CLASS_CACHE.get(md5Str);
            if (clazz == null) {
                //如果没有就在这里把源码解析成class文件
                clazz = groovyClassLoader.parseClass(codeSource);
                //键值对缓存到Map中
                CLASS_CACHE.putIfAbsent(md5Str, clazz);
            }
            //返回class文件
            return clazz;
        } catch (Exception e) {
            return groovyClassLoader.parseClass(codeSource);
        }
    }


    public void injectService(Object instance) {

    }

}
