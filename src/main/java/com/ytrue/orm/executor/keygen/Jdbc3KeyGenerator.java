package com.ytrue.orm.executor.keygen;

import com.ytrue.orm.executor.Executor;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.reflection.MetaObject;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.type.TypeHandler;
import com.ytrue.orm.type.TypeHandlerRegistry;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/9/1 14:45
 * @description 主要用于数据库的自增主键, 比如mysql，postgreSql
 */
public class Jdbc3KeyGenerator implements KeyGenerator {
    @Override
    public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        // Do Nothing
    }

    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {

    }


    public void processBatch(MappedStatement ms, Statement stmt, List<Object> parameters) {
        // 有的时候表会生成主键，这时候就可以用Statement的getGeneratedKeys()方法来获取这个自动生成的主键的值了。
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            final Configuration configuration = ms.getConfiguration();
            final TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            // 获取要填入的key
            final String[] keyProperties = ms.getKeyProperties();
            final ResultSetMetaData rsmd = rs.getMetaData();
            TypeHandler<?>[] typeHandlers = null;

            if (keyProperties != null && rsmd.getColumnCount() >= keyProperties.length) {
                for (Object parameter : parameters) {
                    // there should be one row for each statement (also one for each parameter)
                    if (!rs.next()) {
                        break;
                    }
                    final MetaObject metaParam = configuration.newMetaObject(parameter);
                    if (typeHandlers == null) {
                        // 先取得类型处理器
                        typeHandlers = getTypeHandlers(typeHandlerRegistry, metaParam, keyProperties);
                    }
                    // 填充键值
                    populateKeys(rs, metaParam, keyProperties, typeHandlers);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
        }
    }


    /**
     * 获取类型处理器
     *
     * @param typeHandlerRegistry
     * @param metaParam
     * @param keyProperties
     * @return
     */
    private TypeHandler<?>[] getTypeHandlers(TypeHandlerRegistry typeHandlerRegistry, MetaObject metaParam, String[] keyProperties) {
        TypeHandler<?>[] typeHandlers = new TypeHandler<?>[keyProperties.length];
        for (int i = 0; i < keyProperties.length; i++) {
            if (metaParam.hasSetter(keyProperties[i])) {
                Class<?> keyPropertyType = metaParam.getSetterType(keyProperties[i]);
                TypeHandler<?> th = typeHandlerRegistry.getTypeHandler(keyPropertyType, null);
                typeHandlers[i] = th;
            }
        }
        return typeHandlers;
    }

    /**
     * 填充键值
     *
     * @param rs
     * @param metaParam
     * @param keyProperties
     * @param typeHandlers
     * @throws SQLException
     */
    private void populateKeys(ResultSet rs, MetaObject metaParam, String[] keyProperties, TypeHandler<?>[] typeHandlers) throws SQLException {
        for (int i = 0; i < keyProperties.length; i++) {
            TypeHandler<?> th = typeHandlers[i];
            if (th != null) {
                Object value = th.getResult(rs, i + 1);
                metaParam.setValue(keyProperties[i], value);
            }
        }
    }
}
