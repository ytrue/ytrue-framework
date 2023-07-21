package com.ytrue.orm.binding;

import com.ytrue.orm.session.SqlSession;

import java.lang.reflect.Proxy;

/**
 * @author ytrue
 * @date 2022/8/11 15:09
 * @description 映射器代理工厂
 */
public class MapperProxyFactory<T> {

    private final Class<T> mapperInterface;

    public MapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    /**
     * 生成代理对象
     *
     * @param sqlSession
     * @return
     */
    public T newInstance(SqlSession sqlSession) {
        final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, null);
        return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[]{mapperInterface}, mapperProxy);
    }

}
