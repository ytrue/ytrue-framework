package com.ytrue.orm.binding;

import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.SqlCommandType;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.SqlSession;
import lombok.Getter;

import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2022/8/12 09:25
 * @description 映射器方法
 */
public class MapperMethod {

    private final SqlCommand command;

    public MapperMethod(Class<?> mapperInterface, Method method, Configuration configuration) {
        this.command = new SqlCommand(configuration, mapperInterface, method);
    }

    /**
     * 执行方法
     *
     * @param sqlSession
     * @param args
     * @return
     */
    public Object execute(SqlSession sqlSession, Object[] args) {
        Object result = null;
        switch (command.getType()) {
            case INSERT:
                break;
            case DELETE:
                break;
            case UPDATE:
                break;
            case SELECT:
                result = sqlSession.selectOne(command.getName(), args);
                break;
            default:
                throw new RuntimeException("Unknown execution method for: " + command.getName());
        }
        return result;
    }

    /**
     * SQL 指令
     */
    @Getter
    public static class SqlCommand {

        /**
         * 名称
         */
        private final String name;
        /**
         * sql类型
         */
        private final SqlCommandType type;

        /**
         * 构造方法 主要方法 构建 SqlCommand 给 name 和 type 赋值
         *
         * @param configuration
         * @param mapperInterface
         * @param method
         */
        public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
            // 获取 statementName  包名 + 类名 + 方法名称
            String statementName = mapperInterface.getName() + "." + method.getName();
            // 去map 里获取对应的 MappedStatement
            MappedStatement ms = configuration.getMappedStatement(statementName);
            // 赋值操作
            name = ms.getId();
            type = ms.getSqlCommandType();
        }
    }
}
