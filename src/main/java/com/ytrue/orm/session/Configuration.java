package com.ytrue.orm.session;

import com.ytrue.orm.binding.MapperRegistry;
import com.ytrue.orm.datasource.druid.DruidDataSourceFactory;
import com.ytrue.orm.datasource.pooled.PooledDataSourceFactory;
import com.ytrue.orm.datasource.unpooled.UnpooledDataSourceFactory;
import com.ytrue.orm.mapping.Environment;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.transaction.jdbc.JdbcTransactionFactory;
import com.ytrue.orm.transaction.type.TypeAliasRegistry;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2022/8/11 16:15
 * @description MyBatis所有的配置信息都保存在Configuration对象之中，配置文件中的大部分配置都会存储到该类中。
 */
public class Configuration {

    /**
     * 环境
     */
    @Getter
    @Setter
    protected Environment environment;

    /**
     * 实例化注册器
     */
    protected MapperRegistry mapperRegistry = new MapperRegistry(this);

    /**
     * 映射的语句，存在Map里
     */
    protected final Map<String, MappedStatement> mappedStatements = new HashMap<>();


    /**
     * 类型别名注册机
     */
    @Getter
    protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();

    public Configuration() {
        // 注册jdbc事务管理器
        typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
        // 注册druid的数据源工厂
        typeAliasRegistry.registerAlias("DRUID", DruidDataSourceFactory.class);

        typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);
        typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
    }

    /**
     * 扫描批量加入
     *
     * @param packageName
     */
    public void addMappers(String packageName) {
        mapperRegistry.addMappers(packageName);
    }

    /**
     * 想mapper注册器 追加
     *
     * @param type
     * @param <T>
     */
    public <T> void addMapper(Class<T> type) {
        mapperRegistry.addMapper(type);
    }

    /**
     * 获取 dao的代理类
     *
     * @param type
     * @param sqlSession
     * @param <T>
     * @return
     */
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        return mapperRegistry.getMapper(type, sqlSession);
    }

    /**
     * 是否 dao.class 是否存在
     *
     * @param type
     * @return
     */
    public boolean hasMapper(Class<?> type) {
        return mapperRegistry.hasMapper(type);
    }

    /**
     * mappedStatements map 追加，相同的key会覆盖
     *
     * @param ms
     */
    public void addMappedStatement(MappedStatement ms) {
        mappedStatements.put(ms.getId(), ms);
    }

    /**
     * mappedStatements map 获取
     *
     * @param id
     * @return
     */
    public MappedStatement getMappedStatement(String id) {
        return mappedStatements.get(id);
    }
}
