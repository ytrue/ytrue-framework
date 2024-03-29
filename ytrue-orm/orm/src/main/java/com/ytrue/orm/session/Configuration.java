package com.ytrue.orm.session;

import com.ytrue.orm.binding.MapperRegistry;
import com.ytrue.orm.cache.Cache;
import com.ytrue.orm.cache.decorators.FifoCache;
import com.ytrue.orm.cache.impl.PerpetualCache;
import com.ytrue.orm.datasource.druid.DruidDataSourceFactory;
import com.ytrue.orm.datasource.pooled.PooledDataSourceFactory;
import com.ytrue.orm.datasource.unpooled.UnpooledDataSourceFactory;
import com.ytrue.orm.executor.CachingExecutor;
import com.ytrue.orm.executor.Executor;
import com.ytrue.orm.executor.SimpleExecutor;
import com.ytrue.orm.executor.keygen.KeyGenerator;
import com.ytrue.orm.executor.parameter.ParameterHandler;
import com.ytrue.orm.executor.resultset.DefaultResultSetHandler;
import com.ytrue.orm.executor.resultset.ResultSetHandler;
import com.ytrue.orm.executor.statement.PreparedStatementHandler;
import com.ytrue.orm.executor.statement.StatementHandler;
import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.Environment;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.ResultMap;
import com.ytrue.orm.plugin.Interceptor;
import com.ytrue.orm.plugin.InterceptorChain;
import com.ytrue.orm.reflection.MetaObject;
import com.ytrue.orm.reflection.factory.DefaultObjectFactory;
import com.ytrue.orm.reflection.factory.ObjectFactory;
import com.ytrue.orm.reflection.wrapper.DefaultObjectWrapperFactory;
import com.ytrue.orm.reflection.wrapper.ObjectWrapperFactory;
import com.ytrue.orm.scripting.LanguageDriver;
import com.ytrue.orm.scripting.LanguageDriverRegistry;
import com.ytrue.orm.scripting.xmltags.XMLLanguageDriver;
import com.ytrue.orm.transaction.Transaction;
import com.ytrue.orm.transaction.jdbc.JdbcTransactionFactory;
import com.ytrue.orm.type.TypeAliasRegistry;
import com.ytrue.orm.type.TypeHandlerRegistry;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author ytrue
 * @date 2022/8/11 16:15
 * @description MyBatis所有的配置信息都保存在Configuration对象之中，配置文件中的大部分配置都会存储到该类中。
 */
public class Configuration {


    /**
     * 使用key生成
     */
    protected boolean useGeneratedKeys = false;

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
     * 结果映射，存在Map里
     */
    protected final Map<String, ResultMap> resultMaps = new HashMap<>();

    /**
     * 主键索引
     */
    protected final Map<String, KeyGenerator> keyGenerators = new HashMap<>();

    /**
     * 类型别名注册机
     */
    @Getter
    protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();


    /**
     * 加载过的 mapper.xml
     */
    protected final Set<String> loadedResources = new HashSet<>();


    /**
     * 对象工厂和对象包装器工厂
     */
    @Getter
    protected ObjectFactory objectFactory = new DefaultObjectFactory();
    protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();


    /**
     * 脚本语言注册器
     */
    @Getter
    protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();

    /**
     * 类型处理器注册机
     */
    @Getter
    protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();

    @Getter
    protected String databaseId;

    /**
     * 插件拦截器链
     */
    protected final InterceptorChain interceptorChain = new InterceptorChain();

    /**
     * 缓存机制，默认不配置的情况是 SESSION
     */
    @Setter
    @Getter
    protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION;


    /**
     * 默认启用缓存，cacheEnabled = true/false
     */
    protected boolean cacheEnabled = true;


    /**
     * 缓存,存在Map里
     */
    protected final Map<String, Cache> caches = new HashMap<>();


    public Configuration() {
        // 注册jdbc事务管理器
        typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
        // 注册druid的数据源工厂
        typeAliasRegistry.registerAlias("DRUID", DruidDataSourceFactory.class);
        typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);
        typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);

        typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
        typeAliasRegistry.registerAlias("FIFO", FifoCache.class);

        languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
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


    /**
     * 创建结果集处理器
     */
    public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        return new DefaultResultSetHandler(executor, mappedStatement, resultHandler, rowBounds, boundSql);
    }

    /**
     * 生产执行器
     */
    public Executor newExecutor(Transaction transaction) {
        Executor executor = new SimpleExecutor(this, transaction);
        // 配置开启缓存，创建 CachingExecutor(默认就是有缓存)装饰者模式
        if (cacheEnabled) {
            executor = new CachingExecutor(executor);
        }
        return executor;
    }

    /**
     * 创建语句处理器
     */
    public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        // 创建语句处理器，Mybatis 这里加了路由 STATEMENT、PREPARED、CALLABLE 我们默认只根据预处理进行实例化
        StatementHandler statementHandler = new PreparedStatementHandler(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
        // 嵌入插件，代理对象
        statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
        return statementHandler;
    }

    /**
     * mapper是否加载过了
     *
     * @param resource
     * @return
     */
    public boolean isResourceLoaded(String resource) {
        return loadedResources.contains(resource);
    }

    /**
     * 添加已经加载的mapper文件
     *
     * @param resource
     */
    public void addLoadedResource(String resource) {
        loadedResources.add(resource);
    }


    /**
     * 创建元对象
     *
     * @param object
     * @return
     */
    public MetaObject newMetaObject(Object object) {
        return MetaObject.forObject(object, objectFactory, objectWrapperFactory);
    }


    /**
     * 参数处理器
     *
     * @param mappedStatement
     * @param parameterObject
     * @param boundSql
     * @return
     */
    public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
        // 创建参数处理器
        ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
        return parameterHandler;
    }


    public LanguageDriver getDefaultScriptingLanguageInstance() {
        return languageRegistry.getDefaultDriver();
    }


    /**
     * 加添结果映射
     *
     * @param resultMap
     */
    public void addResultMap(ResultMap resultMap) {
        resultMaps.put(resultMap.getId(), resultMap);
    }


    /**
     * 根据id获取结果映射
     *
     * @param id
     * @return
     */
    public ResultMap getResultMap(String id) {
        return resultMaps.get(id);
    }


    /**
     * 添加KeyGenerator
     *
     * @param id
     * @param keyGenerator
     */
    public void addKeyGenerator(String id, KeyGenerator keyGenerator) {
        keyGenerators.put(id, keyGenerator);
    }

    /**
     * 根据id获取 KeyGenerator
     *
     * @param id
     * @return
     */
    public KeyGenerator getKeyGenerator(String id) {
        return keyGenerators.get(id);
    }

    /**
     * 判断id是否存在 KeyGenerator
     *
     * @param id
     * @return
     */
    public boolean hasKeyGenerator(String id) {
        return keyGenerators.containsKey(id);
    }

    /**
     * 是否使用生成key
     *
     * @return
     */
    public boolean isUseGeneratedKeys() {
        return useGeneratedKeys;
    }

    /**
     * 设置
     *
     * @param useGeneratedKeys
     */
    public void setUseGeneratedKeys(boolean useGeneratedKeys) {
        this.useGeneratedKeys = useGeneratedKeys;
    }


    /**
     * 添加拦截器
     *
     * @param interceptorInstance
     */
    public void addInterceptor(Interceptor interceptorInstance) {
        interceptorChain.addInterceptor(interceptorInstance);
    }


    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public void addCache(Cache cache) {
        caches.put(cache.getId(), cache);
    }

    public Cache getCache(String id) {
        return caches.get(id);
    }
}
