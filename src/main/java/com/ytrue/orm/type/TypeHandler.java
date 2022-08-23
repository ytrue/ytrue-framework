package com.ytrue.orm.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author ytrue
 * @date 2022/8/23 16:28
 * @description 类型处理器
 */
public interface TypeHandler<T> {

    /**
     * 设置参数
     *
     * @param ps
     * @param i
     * @param parameter
     * @param jdbcType
     * @throws SQLException
     */
    void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

}
