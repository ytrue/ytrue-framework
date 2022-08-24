package com.ytrue.orm.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author ytrue
 * @date 2022/8/24 14:24
 * @description 字符串类型处理器
 */
public class StringTypeHandler extends BaseTypeHandler<String> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        // 设置参数
        ps.setString(i, parameter);
    }
}
