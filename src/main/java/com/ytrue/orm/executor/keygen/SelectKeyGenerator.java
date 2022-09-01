package com.ytrue.orm.executor.keygen;

import com.ytrue.orm.executor.Executor;
import com.ytrue.orm.mapping.MappedStatement;
import com.ytrue.orm.reflection.MetaObject;
import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.session.RowBounds;

import java.sql.Statement;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/9/1 14:14
 * @description 主要用于数据库不支持自增主键的情况，比如oracle，db2
 */
public class SelectKeyGenerator implements KeyGenerator {

    public static final String SELECT_KEY_SUFFIX = "!selectKey";
    /**
     * 是在 processBefore执行还是在processAfter执行
     */
    private boolean executeBefore;
    private MappedStatement keyStatement;

    public SelectKeyGenerator(MappedStatement keyStatement, boolean executeBefore) {
        this.executeBefore = executeBefore;
        this.keyStatement = keyStatement;
    }

    @Override
    public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        if (executeBefore) {
            processGeneratedKeys(executor, ms, parameter);
        }
    }

    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        if (!executeBefore) {
            processGeneratedKeys(executor, ms, parameter);
        }
    }


    private void processGeneratedKeys(Executor executor, MappedStatement ms, Object parameter) {
        try {
            if (parameter != null && keyStatement != null && keyStatement.getKeyProperties() != null) {
                // 获取  <selectKey keyProperty="id" 的 keyProperty
                String[] keyProperties = keyStatement.getKeyProperties();
                final Configuration configuration = ms.getConfiguration();
                // 要进行反射了
                final MetaObject metaParam = configuration.newMetaObject(parameter);
                // keyProperty为空就不处理了
                if (keyProperties != null) {
                    // 获取执行器
                    Executor keyExecutor = configuration.newExecutor(executor.getTransaction());
                    // 执行sql语句
                    List<Object> values = keyExecutor.query(keyStatement, parameter, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER);
                    // 获取值只有一个才是正确的
                    if (values.size() == 0) {
                        throw new RuntimeException("SelectKey returned no data.");
                    } else if (values.size() > 1) {
                        throw new RuntimeException("SelectKey returned more than one value.");
                    } else {
                        // 对获取的值反射
                        MetaObject metaResult = configuration.newMetaObject(values.get(0));
                        if (keyProperties.length == 1) {
                            // 设置值
                            if (metaResult.hasGetter(keyProperties[0])) {
                                setValue(metaParam, keyProperties[0], metaResult.getValue(keyProperties[0]));
                            } else {
                                setValue(metaParam, keyProperties[0], values.get(0));
                            }
                        } else {
                            // 处理多个值
                            handleMultipleProperties(keyProperties, metaParam, metaResult);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error selecting key or setting result to parameter object. Cause: " + e);
        }
    }

    /**
     * 多个值处理
     *
     * @param keyProperties
     * @param metaParam
     * @param metaResult
     */
    private void handleMultipleProperties(
            String[] keyProperties,
            MetaObject metaParam,
            MetaObject metaResult
    ) {
        String[] keyColumns = keyStatement.getKeyColumns();

        if (keyColumns == null || keyColumns.length == 0) {
            for (String keyProperty : keyProperties) {
                setValue(metaParam, keyProperty, metaResult.getValue(keyProperty));
            }
        } else {
            if (keyColumns.length != keyProperties.length) {
                throw new RuntimeException("If SelectKey has key columns, the number must match the number of key properties.");
            }
            for (int i = 0; i < keyProperties.length; i++) {
                setValue(metaParam, keyProperties[i], metaResult.getValue(keyColumns[i]));
            }
        }
    }

    /**
     * 对象赋值
     *
     * @param metaParam
     * @param property
     * @param value
     */
    private void setValue(MetaObject metaParam, String property, Object value) {
        if (metaParam.hasSetter(property)) {
            metaParam.setValue(property, value);
        } else {
            throw new RuntimeException("No setter found for the keyProperty '" + property + "' in " + metaParam.getOriginalObject().getClass().getName() + ".");
        }
    }
}
