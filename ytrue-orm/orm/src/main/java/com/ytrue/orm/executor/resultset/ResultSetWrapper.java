package com.ytrue.orm.executor.resultset;

import com.ytrue.orm.io.Resources;
import com.ytrue.orm.mapping.ResultMap;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.type.JdbcType;
import com.ytrue.orm.type.TypeHandler;
import com.ytrue.orm.type.TypeHandlerRegistry;
import lombok.Getter;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * @author ytrue
 * @date 2022/8/25 16:07
 * @description 结果集包装器
 */
public class ResultSetWrapper {

    /**
     * ResultSet
     */
    @Getter
    private final ResultSet resultSet;

    /**
     * 类型处理注册器
     */
    private final TypeHandlerRegistry typeHandlerRegistry;

    /**
     * SELECT id, userId, userName, userHead 的查询字段
     */
    @Getter
    private final List<String> columnNames = new ArrayList<>();

    /**
     * SELECT id, userId, userName, userHead 的查询字段的java类型
     */
    @Getter
    private final List<String> classNames = new ArrayList<>();

    /**
     * SELECT id, userId, userName, userHead 的查询字段的jdbc类型
     */
    @Getter
    private final List<JdbcType> jdbcTypes = new ArrayList<>();

    /**
     * typeHandlerMap
     */
    private final Map<String, Map<Class<?>, TypeHandler<?>>> typeHandlerMap = new HashMap<>();

    /**
     * mappedColumnNamesMap
     */
    private Map<String, List<String>> mappedColumnNamesMap = new HashMap<>();

    /**
     * mappedColumnNamesMap
     */
    private Map<String, List<String>> unMappedColumnNamesMap = new HashMap<>();


    public ResultSetWrapper(ResultSet rs, Configuration configuration) throws SQLException {

        this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();

        this.resultSet = rs;

        final ResultSetMetaData metaData = rs.getMetaData();

        // select语句 查询字段的个数, 如 SELECT id, userId, userName, userHead form xxx 这里就是三个
        final int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            // 获取 select 第一个 这里是id
            columnNames.add(metaData.getColumnLabel(i));
            // 获取 字段的 jdbc类型，这里id是bigint = -5
            jdbcTypes.add(JdbcType.forCode(metaData.getColumnType(i)));
            // 获取 字段的java类型 ，这里是long
            classNames.add(metaData.getColumnClassName(i));
        }
    }

    /**
     * 获取类型处理器
     *
     * @param propertyType
     * @param columnName
     * @return
     */
    public TypeHandler<?> getTypeHandler(Class<?> propertyType, String columnName) {

        TypeHandler<?> handler = null;

        Map<Class<?>, TypeHandler<?>> columnHandlers = typeHandlerMap.get(columnName);

        if (columnHandlers == null) {
            columnHandlers = new HashMap<>();
            typeHandlerMap.put(columnName, columnHandlers);
        } else {
            handler = columnHandlers.get(propertyType);
        }
        if (handler == null) {
            handler = typeHandlerRegistry.getTypeHandler(propertyType, null);
            columnHandlers.put(propertyType, handler);
        }
        return handler;
    }


    private Class<?> resolveClass(String className) {
        try {
            return Resources.classForName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 加载加载映射和未映射的列名
     *
     * @param resultMap
     * @param columnPrefix
     * @throws SQLException
     */
    private void loadMappedAndUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
        List<String> mappedColumnNames = new ArrayList<>();
        List<String> unmappedColumnNames = new ArrayList<>();

        final String upperColumnPrefix = columnPrefix == null ? null : columnPrefix.toUpperCase(Locale.ENGLISH);
        final Set<String> mappedColumns = prependPrefixes(resultMap.getMappedColumns(), upperColumnPrefix);
        for (String columnName : columnNames) {
            final String upperColumnName = columnName.toUpperCase(Locale.ENGLISH);
            if (mappedColumns.contains(upperColumnName)) {
                mappedColumnNames.add(upperColumnName);
            } else {
                unmappedColumnNames.add(columnName);
            }
        }
        mappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), mappedColumnNames);
        unMappedColumnNamesMap.put(getMapKey(resultMap, columnPrefix), unmappedColumnNames);
    }

    /**
     * 获取映射的列名
     *
     * @param resultMap
     * @param columnPrefix
     * @return
     * @throws SQLException
     */
    public List<String> getMappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
        List<String> mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
        if (mappedColumnNames == null) {
            loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);
            mappedColumnNames = mappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
        }
        return mappedColumnNames;
    }

    /**
     * 获取未映射的列名
     *
     * @param resultMap
     * @param columnPrefix
     * @return
     * @throws SQLException
     */
    public List<String> getUnmappedColumnNames(ResultMap resultMap, String columnPrefix) throws SQLException {
        // getMapKey(resultMap, columnPrefix) 获取id名称
        List<String> unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));

        // 第一次会走这里的
        if (unMappedColumnNames == null) {
            loadMappedAndUnmappedColumnNames(resultMap, columnPrefix);

            unMappedColumnNames = unMappedColumnNamesMap.get(getMapKey(resultMap, columnPrefix));
        }
        return unMappedColumnNames;
    }

    private String getMapKey(ResultMap resultMap, String columnPrefix) {
        return resultMap.getId() + ":" + columnPrefix;
    }

    private Set<String> prependPrefixes(Set<String> columnNames, String prefix) {
        if (columnNames == null || columnNames.isEmpty() || prefix == null || prefix.length() == 0) {
            return columnNames;
        }
        final Set<String> prefixed = new HashSet<String>();
        for (String columnName : columnNames) {
            prefixed.add(prefix + columnName);
        }
        return prefixed;
    }

}
