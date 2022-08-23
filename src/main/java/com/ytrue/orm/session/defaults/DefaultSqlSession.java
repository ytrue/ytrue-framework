package com.ytrue.orm.session.defaults;

import com.ytrue.orm.executor.Executor;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.SqlSession;

import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/11 15:25
 * @description 默认SqlSession实现类
 */
public class DefaultSqlSession implements SqlSession {

    private Configuration configuration;
    private Executor executor;

    public DefaultSqlSession(Configuration configuration, Executor executor) {
        this.configuration = configuration;
        this.executor = executor;
    }

    @Override
    public <T> T selectOne(String statement) {
        return (T) ("你被代理了！" + statement);
    }

    @Override
    public <T> T selectOne(String statement, Object parameter) {
        try {
            MappedStatement ms = configuration.getMappedStatement(statement);
            // 交给 executor 处理
            List<T> list = executor.query(ms, parameter, Executor.NO_RESULT_HANDLER, ms.getSqlSource().getBoundSql(parameter));
            return list.get(0);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取传入的class 的代理对象
     *
     * @param type Mapper interface class
     * @param <T>
     * @return
     */
    @Override
    public <T> T getMapper(Class<T> type) {
        return configuration.getMapper(type, this);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }
}
