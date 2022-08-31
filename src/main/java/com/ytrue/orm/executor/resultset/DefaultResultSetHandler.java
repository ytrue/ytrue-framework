package com.ytrue.orm.executor.resultset;

import com.ytrue.orm.executor.Executor;
import com.ytrue.orm.executor.result.DefaultResultContext;
import com.ytrue.orm.executor.result.DefaultResultHandler;
import com.ytrue.orm.mapping.BoundSql;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.mapping.ResultMap;
import com.ytrue.orm.mapping.ResultMapping;
import com.ytrue.orm.reflection.MetaClass;
import com.ytrue.orm.reflection.MetaObject;
import com.ytrue.orm.reflection.factory.ObjectFactory;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.ResultHandler;
import com.ytrue.orm.session.RowBounds;
import com.ytrue.orm.type.TypeHandler;
import com.ytrue.orm.type.TypeHandlerRegistry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author ytrue
 * @date 2022/8/18 16:48
 * @description 默认Map结果处理器
 */
public class DefaultResultSetHandler implements ResultSetHandler {

    private static final Object NO_VALUE = new Object();

    private final Configuration configuration;
    private final MappedStatement mappedStatement;
    private final RowBounds rowBounds;
    private final ResultHandler resultHandler;
    private final BoundSql boundSql;
    private final TypeHandlerRegistry typeHandlerRegistry;
    private final ObjectFactory objectFactory;

    public DefaultResultSetHandler(Executor executor, MappedStatement mappedStatement, ResultHandler resultHandler, RowBounds rowBounds, BoundSql boundSql) {
        this.configuration = mappedStatement.getConfiguration();
        this.rowBounds = rowBounds;
        this.boundSql = boundSql;
        this.mappedStatement = mappedStatement;
        this.resultHandler = resultHandler;
        this.objectFactory = configuration.getObjectFactory();
        this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    }


    @Override
    @SuppressWarnings("unchecked")
    public List<Object> handleResultSets(Statement stmt) throws SQLException {
        final List<Object> multipleResults = new ArrayList<>();
        int resultSetCount = 0;

        // ResultSet包装
        ResultSetWrapper rsw = new ResultSetWrapper(stmt.getResultSet(), configuration);

        List<ResultMap> resultMaps = mappedStatement.getResultMaps();

        // 如果包装不等于null，resultMaps大于0
        while (rsw != null && resultMaps.size() > resultSetCount) {
            // 获取ResultMap
            ResultMap resultMap = resultMaps.get(resultSetCount);
            // 处理结果
            handleResultSet(rsw, resultMap, multipleResults, null);
            // 获取下一个结果
            rsw = getNextResultSet(stmt);
            resultSetCount++;
        }

        return multipleResults.size() == 1 ? (List<Object>) multipleResults.get(0) : multipleResults;
    }


    private ResultSetWrapper getNextResultSet(Statement stmt) throws SQLException {
        // 使此方法能够容忍不良的 JDBC 驱动程序
        try {
            if (stmt.getConnection().getMetaData().supportsMultipleResultSets()) {
                // Crazy Standard JDBC way of determining if there are more results
                if (!((!stmt.getMoreResults()) && (stmt.getUpdateCount() == -1))) {
                    ResultSet rs = stmt.getResultSet();
                    return rs != null ? new ResultSetWrapper(rs, configuration) : null;
                }
            }
        } catch (Exception ignore) {
            // Intentionally ignored.
        }
        return null;
    }

    private void handleResultSet(ResultSetWrapper rsw, ResultMap resultMap, List<Object> multipleResults, ResultMapping parentMapping) throws SQLException {
        if (resultHandler == null) {
            // 1. 新创建结果处理器 里面就一个list
            DefaultResultHandler defaultResultHandler = new DefaultResultHandler(objectFactory);
            // 2. 封装数据
            handleRowValuesForSimpleResultMap(rsw, resultMap, defaultResultHandler, rowBounds, null);
            // 3. 保存结果
            multipleResults.add(defaultResultHandler.getResultList());
        }
    }

    private void handleRowValuesForSimpleResultMap(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
        DefaultResultContext resultContext = new DefaultResultContext();
        // 如果 resultContext.getResultCount() < rowBounds.getLimit() 并且 ResultSet.next() == true 才会指定
        while (resultContext.getResultCount() < rowBounds.getLimit() && rsw.getResultSet().next()) {
            // 获取行值
            Object rowValue = getRowValue(rsw, resultMap);
            // 再次调取
            callResultHandler(resultHandler, resultContext, rowValue);
        }
    }

    private void callResultHandler(ResultHandler resultHandler, DefaultResultContext resultContext, Object rowValue) {
        resultContext.nextResultObject(rowValue);
        resultHandler.handleResult(resultContext);
    }

    /**
     * 获取一行的值
     *
     * @param rsw
     * @param resultMap
     * @return
     * @throws SQLException
     */
    private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap) throws SQLException {
        // 根据返回类型，实例化对象
        Object resultObject = createResultObject(rsw, resultMap, null);

        // 如果返回结果的对象不等于null 并且 它不存在类型处理器
        if (resultObject != null && !typeHandlerRegistry.hasTypeHandler(resultMap.getType())) {
            final MetaObject metaObject = configuration.newMetaObject(resultObject);
            // 自动映射：把每列的值都赋到对应的字段上
            applyAutomaticMappings(rsw, resultMap, metaObject, null);
            // Map映射：根据映射类型赋值到字段
            applyPropertyMappings(rsw, resultMap, metaObject, null);
        }
        return resultObject;
    }


    /**
     * 获取对应的类型处理器获取数据再通过反射给对象赋值
     *
     * @param rsw
     * @param resultMap
     * @param metaObject
     * @param columnPrefix
     * @return
     * @throws SQLException
     */
    private boolean applyAutomaticMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String columnPrefix) throws SQLException {
        // 获取未映射的列名
        final List<String> unmappedColumnNames = rsw.getUnmappedColumnNames(resultMap, columnPrefix);

        boolean foundValues = false;

        for (String columnName : unmappedColumnNames) {
            String propertyName = columnName;
            if (columnPrefix != null && !columnPrefix.isEmpty()) {
                // When columnPrefix is specified,ignore columns without the prefix.
                if (columnName.toUpperCase(Locale.ENGLISH).startsWith(columnPrefix)) {
                    propertyName = columnName.substring(columnPrefix.length());
                } else {
                    continue;
                }
            }
            // 查找这个字段的属性
            final String property = metaObject.findProperty(propertyName, false);
            // 如果这个属性存在并且存在set方法
            if (property != null && metaObject.hasSetter(property)) {
                // 获取set类型
                final Class<?> propertyType = metaObject.getSetterType(property);
                // 如果存在这个类型处理器
                if (typeHandlerRegistry.hasTypeHandler(propertyType)) {
                    // 获取对应的类型处理器
                    final TypeHandler<?> typeHandler = rsw.getTypeHandler(propertyType, columnName);
                    // 使用 TypeHandler 取得结果
                    final Object value = typeHandler.getResult(rsw.getResultSet(), columnName);
                    if (value != null) {
                        foundValues = true;
                    }
                    if (value != null || !propertyType.isPrimitive()) {
                        // 通过反射工具类设置属性值
                        metaObject.setValue(property, value);
                    }
                }
            }
        }
        return foundValues;
    }


    /**
     * 创建结果对象
     *
     * @param rsw
     * @param resultMap
     * @param columnPrefix
     * @return
     * @throws SQLException
     */
    private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix) throws SQLException {
        // 构造函数参数类型
        final List<Class<?>> constructorArgTypes = new ArrayList<>();
        // 构造函数参数
        final List<Object> constructorArgs = new ArrayList<>();
        return createResultObject(rsw, resultMap, constructorArgTypes, constructorArgs, columnPrefix);
    }

    /**
     * 创建结果
     *
     * @param rsw
     * @param resultMap
     * @param constructorArgTypes
     * @param constructorArgs
     * @param columnPrefix
     * @return
     * @throws SQLException
     */
    private Object createResultObject(ResultSetWrapper rsw, ResultMap resultMap, List<Class<?>> constructorArgTypes, List<Object> constructorArgs, String columnPrefix) throws SQLException {
        // 获取返回结果的类型
        final Class<?> resultType = resultMap.getType();
        // 构建MetaClass
        final MetaClass metaType = MetaClass.forClass(resultType);
        // 如果是接口 或者 有构造函数那么就可以创建这个对象
        if (resultType.isInterface() || metaType.hasDefaultConstructor()) {
            // 普通的Bean对象类型
            return objectFactory.create(resultType);
        }
        throw new RuntimeException("Do not know how to create an instance of " + resultType);
    }


    /**
     * Map映射：根据映射类型赋值到字段
     *
     * @param rsw
     * @param resultMap
     * @param metaObject
     * @param columnPrefix
     * @return
     * @throws SQLException
     */
    private boolean applyPropertyMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject, String columnPrefix) throws SQLException {
        // 获取映射的列名
        final List<String> mappedColumnNames = rsw.getMappedColumnNames(resultMap, columnPrefix);
        boolean foundValues = false;

        final List<ResultMapping> propertyMappings = resultMap.getPropertyResultMappings();
        for (ResultMapping propertyMapping : propertyMappings) {
            final String column = propertyMapping.getColumn();
            if (column != null && mappedColumnNames.contains(column.toUpperCase(Locale.ENGLISH))) {
                // 获取值
                final TypeHandler<?> typeHandler = propertyMapping.getTypeHandler();
                Object value = typeHandler.getResult(rsw.getResultSet(), column);
                // 设置值
                final String property = propertyMapping.getProperty();
                if (value != NO_VALUE && property != null && value != null) {
                    // 通过反射工具类设置属性值
                    metaObject.setValue(property, value);
                    foundValues = true;
                }
            }
        }
        return foundValues;
    }
}
