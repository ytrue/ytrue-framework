package com.ytrue.orm.session.defaults;

import com.ytrue.orm.binding.MapperRegistry;
import com.ytrue.orm.session.SqlSession;
import lombok.AllArgsConstructor;

/**
 * @author ytrue
 * @date 2022/7/11 11:18
 * @description DefaultSqlSession
 */
@AllArgsConstructor
public class DefaultSqlSession implements SqlSession {

    /**
     * 映射器注册机
     */
    private MapperRegistry mapperRegistry;

    @Override
    public <T> T selectOne(String statement) {
        return null;
    }

    @Override
    public <T> T selectOne(String statement, Object parameter) {
        return (T) ("你被代理了！" + "方法：" + statement + " 入参：" + parameter);
    }

    @Override
    public <T> T getMapper(Class<T> type) {
        return mapperRegistry.getMapper(type, this);
    }
}
