package com.ytrue.orm.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author ytrue
 * @date 2022/8/24 14:24
 * @description long类型处理器
 */
public class LongTypeHandler extends BaseTypeHandler<Long> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, Long parameter, JdbcType jdbcType) throws SQLException {
        // 设置参数
        ps.setLong(i, parameter);
    }
}
