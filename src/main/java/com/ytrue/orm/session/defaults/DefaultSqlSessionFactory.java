package com.ytrue.orm.session.defaults;

import com.ytrue.orm.binding.MapperRegistry;
import com.ytrue.orm.session.SqlSession;
import com.ytrue.orm.session.SqlSessionFactory;
import lombok.AllArgsConstructor;

/**
 * @author ytrue
 * @date 2022/7/11 11:18
 * @description DefaultSqlSessionFactory
 */
@AllArgsConstructor
public class DefaultSqlSessionFactory implements SqlSessionFactory {

    private final MapperRegistry mapperRegistry;

    @Override
    public SqlSession openSession() {
        return new DefaultSqlSession(mapperRegistry);
    }
}
