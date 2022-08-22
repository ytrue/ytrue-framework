package com.ytrue.orm.binding;

import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.SqlSession;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2022/8/11 15:27
 * @description 映射器注册机
 */
public class MapperRegistry {

    private Configuration config;

    /**
     * 将已添加的映射器代理加入到 HashMap
     */
    private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();

    public MapperRegistry(Configuration config) {
        this.config = config;
    }

    /**
     * 获取代理对象
     *
     * @param type
     * @param sqlSession
     * @param <T>
     * @return
     */
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {

        final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);

        if (mapperProxyFactory == null) {
            throw new RuntimeException("Type " + type + " is not known to the MapperRegistry.");
        } else {
            try {
                // 生成代理对象
                return mapperProxyFactory.newInstance(sqlSession);
            } catch (Exception e) {
                throw new RuntimeException("Error getting mapper instance. Cause: " + e, e);
            }
        }
    }

    /**
     * 注册
     *
     * @param type
     * @param <T>
     */
    public <T> void addMapper(Class<T> type) {
        /* Mapper 必须是接口才会注册 */
        if (type.isInterface()) {
            if (hasMapper(type)) {
                // 如果重复添加了，报错
                throw new RuntimeException("Type " + type + " is already known to the MapperRegistry.");
            }
            // 注册映射器代理工厂
            knownMappers.put(type, new MapperProxyFactory<>(type));
        }
    }

    /**
     * 判断是否存在
     *
     * @param type
     * @param <T>
     * @return
     */
    public <T> boolean hasMapper(Class<T> type) {
        return knownMappers.containsKey(type);
    }


    /**
     * 传入包的路径扫码这个包下的 所有类，在把类加入map里面去
     *
     * @param packageName
     */
    public void addMappers(String packageName, Class<?> superType) {
        // 这个要参考mybatis 的ResolverUtil，或者自己做一个
//        ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
//        resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
//        Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
//        for (Class<?> mapperClass : mapperSet) {
//            addMapper(mapperClass);
//        }
    }

    /**
     * @since 3.2.2
     */
    public void addMappers(String packageName) {
        addMappers(packageName, Object.class);
    }

}
