package com.ytrue.orm.builder;

import com.ytrue.orm.cache.Cache;
import com.ytrue.orm.cache.decorators.FifoCache;
import com.ytrue.orm.cache.impl.PerpetualCache;
import com.ytrue.orm.executor.keygen.KeyGenerator;
import com.ytrue.orm.mapping.*;
import com.ytrue.orm.reflection.MetaClass;
import com.ytrue.orm.scripting.LanguageDriver;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.type.TypeHandler;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author ytrue
 * @date 2022/8/25 15:00
 * @description 映射构建器助手，建造者
 */
public class MapperBuilderAssistant extends BaseBuilder {


    private final static String STRING_SPOT = ".";

    /**
     * 命名空间
     */
    @Getter
    @Setter
    private String currentNamespace;

    /**
     * resource内容
     */
    private String resource;

    private Cache currentCache;

    public MapperBuilderAssistant(Configuration configuration, String resource) {
        super(configuration);
        this.resource = resource;
    }


    /**
     * 添加映射器语句
     *
     * @param id
     * @param sqlSource
     * @param sqlCommandType
     * @param parameterType
     * @param resultMap
     * @param resultType
     * @param lang
     * @return
     */
    public MappedStatement addMappedStatement(
            String id,
            SqlSource sqlSource,
            SqlCommandType sqlCommandType,
            Class<?> parameterType,
            String resultMap,
            Class<?> resultType,
            boolean flushCache,
            boolean useCache,
            KeyGenerator keyGenerator,
            String keyProperty,

            LanguageDriver lang
    ) {
        // 给id加上namespace前缀：com.ytrue.mybatis.test.dao.IUserDao.queryUserInfoById
        id = applyCurrentNamespace(id, false);
        //是否是select语句
        boolean isSelect = sqlCommandType == SqlCommandType.SELECT;

        MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlCommandType, sqlSource, resultType);
        statementBuilder.resource(resource);
        statementBuilder.keyGenerator(keyGenerator);
        statementBuilder.keyProperty(keyProperty);

        // 结果映射，给 MappedStatement#resultMaps
        setStatementResultMap(resultMap, resultType, statementBuilder);
        // 设置缓存
        setStatementCache(isSelect, flushCache, useCache, currentCache, statementBuilder);

        MappedStatement statement = statementBuilder.build();
        // 映射语句信息，建造完存放到配置项中
        configuration.addMappedStatement(statement);

        return statement;
    }


    private void setStatementCache(
            boolean isSelect,
            boolean flushCache,
            boolean useCache,
            Cache cache,
            MappedStatement.Builder statementBuilder
    ) {
        flushCache = valueOrDefault(flushCache, !isSelect);
        useCache = valueOrDefault(useCache, isSelect);

        statementBuilder.flushCacheRequired(flushCache);
        statementBuilder.useCache(useCache);
        statementBuilder.cache(cache);
    }


    /**
     * 设置返回map
     *
     * @param resultMap
     * @param resultType
     * @param statementBuilder
     */
    private void setStatementResultMap(
            String resultMap,
            Class<?> resultType,
            MappedStatement.Builder statementBuilder) {
        // 因为暂时还没有在 Mapper XML 中配置 Map 返回结果，所以这里返回的是 null
        resultMap = applyCurrentNamespace(resultMap, true);

        List<ResultMap> resultMaps = new ArrayList<>();

        if (resultMap != null) {
            String[] resultMapNames = resultMap.split(",");
            for (String resultMapName : resultMapNames) {
                resultMaps.add(configuration.getResultMap(resultMapName.trim()));
            }
        }
        /*
         * 通常使用 resultType 即可满足大部分场景
         * <select id="queryUserInfoById" resultType="cn.bugstack.mybatis.test.po.User">
         * 使用 resultType 的情况下，Mybatis 会自动创建一个 ResultMap，基于属性名称映射列到 JavaBean 的属性上。
         */
        else if (resultType != null) {
            ResultMap.Builder inlineResultMapBuilder = new ResultMap.Builder(
                    configuration,
                    statementBuilder.id() + "-Inline",
                    resultType,
                    new ArrayList<>());
            resultMaps.add(inlineResultMapBuilder.build());
        }
        statementBuilder.resultMaps(resultMaps);
    }


    /**
     * 给id加上namespace前缀：com.ytrue.orm.test.dao.IUserDao.queryUserInfoById
     *
     * @param base
     * @param isReference
     * @return
     */
    public String applyCurrentNamespace(String base, boolean isReference) {
        // 如果为空就直接返回null
        if (base == null) {
            return null;
        }

        if (isReference) {
            if (base.contains(STRING_SPOT)) {
                return base;
            }
        } else {
            if (base.startsWith(currentNamespace + STRING_SPOT)) {
                return base;
            }
            if (base.contains(STRING_SPOT)) {
                throw new RuntimeException("Dots are not allowed in element names, please remove it from " + base);
            }
        }
        return currentNamespace + STRING_SPOT + base;
    }


    public ResultMap addResultMap(String id, Class<?> type, List<ResultMapping> resultMappings) {

        // 补全ID全路径，如：com.ytrue.orm.test.dao.IActivityDao + activityMap
        id = applyCurrentNamespace(id, false);

        ResultMap.Builder inlineResultMapBuilder = new ResultMap.Builder(
                configuration,
                id,
                type,
                resultMappings);
        ResultMap resultMap = inlineResultMapBuilder.build();
        configuration.addResultMap(resultMap);
        return resultMap;
    }

    /**
     * step-13 新增方法
     *
     * @param resultType
     * @param property
     * @param column
     * @param flags
     * @return
     */
    public ResultMapping buildResultMapping(
            Class<?> resultType,
            String property,
            String column,
            List<ResultFlag> flags) {

        // 获得resultType 对应字段的类型
        Class<?> javaTypeClass = resolveResultJavaType(resultType, property, null);
        // 获取类型处理器
        TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, null);

        ResultMapping.Builder builder = new ResultMapping.Builder(configuration, property, column, javaTypeClass);

        builder.typeHandler(typeHandlerInstance);
        builder.flags(flags);

        return builder.build();
    }


    /**
     * 获得resultType 对应字段的类型
     *
     * @param resultType
     * @param property
     * @param javaType
     * @return
     */
    private Class<?> resolveResultJavaType(Class<?> resultType, String property, Class<?> javaType) {
        if (javaType == null && property != null) {
            try {
                MetaClass metaResultType = MetaClass.forClass(resultType);
                javaType = metaResultType.getSetterType(property);
            } catch (Exception ignore) {
            }
        }
        if (javaType == null) {
            javaType = Object.class;
        }
        return javaType;
    }


    public Cache useNewCache(Class<? extends Cache> typeClass,
                             Class<? extends Cache> evictionClass,
                             Long flushInterval,
                             Integer size,
                             boolean readWrite,
                             boolean blocking,
                             Properties props) {
        // 判断为null，则用默认值
        typeClass = valueOrDefault(typeClass, PerpetualCache.class);
        evictionClass = valueOrDefault(evictionClass, FifoCache.class);

        // 建造者模式构建 Cache [currentNamespace=com.ytrue.orm.test.dao.IActivityDao]
        Cache cache = new CacheBuilder(currentNamespace)
                .implementation(typeClass)
                .addDecorator(evictionClass)
                .clearInterval(flushInterval)
                .size(size)
                .readWrite(readWrite)
                .blocking(blocking)
                .properties(props)
                .build();

        // 添加缓存
        configuration.addCache(cache);
        currentCache = cache;
        return cache;
    }


    private <T> T valueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }


}
