package com.ytrue.orm.builder;

import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.ResultMap;
import com.ytrue.orm.mapping.SqlCommandType;
import com.ytrue.orm.mapping.SqlSource;
import com.ytrue.orm.scripting.LanguageDriver;
import com.ytrue.orm.session.Configuration;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/8/25 15:00
 * @description 映射构建器助手，建造者
 */
public class MapperBuilderAssistant extends BaseBuilder {


    private final static String STRIN_SPOT = ".";

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
            LanguageDriver lang
    ) {
        // 给id加上namespace前缀：cn.bugstack.mybatis.test.dao.IUserDao.queryUserInfoById
        id = applyCurrentNamespace(id, false);
        MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlCommandType, sqlSource, resultType);

        // 结果映射，给 MappedStatement#resultMaps
        setStatementResultMap(resultMap, resultType, statementBuilder);

        MappedStatement statement = statementBuilder.build();
        // 映射语句信息，建造完存放到配置项中
        configuration.addMappedStatement(statement);

        return statement;
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
            // TODO：暂无Map结果映射配置，本章节不添加此逻辑
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
            if (base.contains(STRIN_SPOT)) {
                return base;
            }
        }
        return currentNamespace + STRIN_SPOT + base;
    }


}