package com.ytrue.orm.type;

import com.ytrue.orm.session.Configuration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author ytrue
 * @date 2022/8/24 14:22
 * @description 类型处理器的基类
 */
public abstract class BaseTypeHandler<T> implements TypeHandler<T> {

    protected Configuration configuration;

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * 设置参数
     *
     * @param ps
     * @param i
     * @param parameter
     * @param jdbcType
     * @throws SQLException
     */
    @Override
    public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        // 定义抽象方法，由子类实现不同类型的属性设置
        setNonNullParameter(ps, i, parameter, jdbcType);
    }

    /**
     * 设置非空参数
     *
     * @param ps
     * @param i
     * @param parameter
     * @param jdbcType
     * @throws SQLException
     */
    protected abstract void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;


    @Override
    public T getResult(ResultSet rs, String columnName) throws SQLException {
        return getNullableResult(rs, columnName);
    }

    /**
     * 获取结果
     *
     * @param rs
     * @param columnName
     * @return
     * @throws SQLException
     */
    protected abstract T getNullableResult(ResultSet rs, String columnName) throws SQLException;


    @Override
    public T getResult(ResultSet rs, int columnIndex) throws SQLException {
        return getNullableResult(rs, columnIndex);
    }

    /**
     * 获取结果
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException
     */
    public abstract T getNullableResult(ResultSet rs, int columnIndex) throws SQLException;
}
