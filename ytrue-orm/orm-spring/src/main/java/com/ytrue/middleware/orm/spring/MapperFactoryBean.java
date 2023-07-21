package com.ytrue.middleware.orm.spring;

import com.ytrue.orm.session.SqlSessionFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author ytrue
 * @date 2022/9/10 12:13
 * @description Mapper 工厂对象
 */
public class MapperFactoryBean<T> implements FactoryBean<T> {

    private Class<T> mapperInterface;
    private SqlSessionFactory sqlSessionFactory;

    public MapperFactoryBean(Class<T> mapperInterface, SqlSessionFactory sqlSessionFactory) {
        this.mapperInterface = mapperInterface;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public T getObject() throws Exception {
        return sqlSessionFactory.openSession().getMapper(mapperInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return mapperInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
