package com.ytrue.orm.binding;

import com.ytrue.orm.session.SqlSession;

import java.lang.reflect.Proxy;

/**
 * @author ytrue
 * @date 2022/7/11 10:59
 * @description MapperProxyFactory
 * 是对 MapperProxy 的包装，
 * 对外提供实例化对象的操作。
 * 当我们后面开始给每个操作数据库的接口映射器注册代理的时候，
 * 就需要使用到这个工厂类了
 */
public class MapperProxyFactory<T> {

    private final Class<T> mapperInterface;

    public MapperProxyFactory(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    public T newInstance(SqlSession sqlSession) {
        // 获取对应的MapperProxy
        final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface);
        // jdk动态代理
        return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[]{mapperInterface}, mapperProxy);
    }

}
