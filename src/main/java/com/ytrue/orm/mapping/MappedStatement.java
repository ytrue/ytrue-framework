package com.ytrue.orm.mapping;

import com.ytrue.orm.executor.keygen.KeyGenerator;
import com.ytrue.orm.scripting.LanguageDriver;
import com.ytrue.orm.session.Configuration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/11 16:17
 * @description MappedStatement维护一条<select | update | delete | insert>节点的封装。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MappedStatement {

    /**
     * 配置文件
     */
    private Configuration configuration;

    /**
     * Dao的类名加方法名称
     */

    private String id;
    /**
     * sql 类型
     */
    private SqlCommandType sqlCommandType;

    /**
     * 绑定Sql
     */
    private SqlSource sqlSource;

    /**
     * 返回类型
     */
    Class<?> resultType;

    /**
     * 脚本语言驱动
     */
    private LanguageDriver lang;

    /**
     * 返回结果
     */
    private List<ResultMap> resultMaps;

    private boolean flushCacheRequired;

    /**
     * step-14 新增
     */
    private KeyGenerator keyGenerator;
    private String[] keyProperties;
    private String[] keyColumns;

    /**
     * <Mapper resource ="resource"></>
     */
    private String resource;



    /**
     * step-11 新增方法
     */
    public BoundSql getBoundSql(Object parameterObject) {
        // 调用 SqlSource#getBoundSql
        return sqlSource.getBoundSql(parameterObject);
    }

    /**
     * 建造者
     */
    public static class Builder {

        private MappedStatement mappedStatement = new MappedStatement();

        public Builder(Configuration configuration, String id, SqlCommandType sqlCommandType, SqlSource sqlSource, Class<?> resultType) {
            mappedStatement.configuration = configuration;
            mappedStatement.id = id;
            mappedStatement.sqlCommandType = sqlCommandType;
            mappedStatement.sqlSource = sqlSource;
            mappedStatement.resultType = resultType;
            mappedStatement.lang = configuration.getDefaultScriptingLanguageInstance();
        }

        public MappedStatement build() {
            assert mappedStatement.configuration != null;
            assert mappedStatement.id != null;
            return mappedStatement;
        }

        public String id() {
            return mappedStatement.id;
        }


        public Builder resource(String resource) {
            mappedStatement.resource = resource;
            return this;
        }

        public Builder keyGenerator(KeyGenerator keyGenerator) {
            mappedStatement.keyGenerator = keyGenerator;
            return this;
        }

        public Builder keyProperty(String keyProperty) {
            mappedStatement.keyProperties = delimitedStringToArray(keyProperty);
            return this;
        }

        public Builder resultMaps(List<ResultMap> resultMaps) {
            mappedStatement.resultMaps = resultMaps;
            return this;
        }

    }

    /**
     * 逗号分割字符串
     *
     * @param in
     * @return
     */
    private static String[] delimitedStringToArray(String in) {
        if (in == null || in.trim().length() == 0) {
            return null;
        } else {
            return in.split(",");
        }
    }

    public boolean isFlushCacheRequired() {
        return flushCacheRequired;
    }

}
