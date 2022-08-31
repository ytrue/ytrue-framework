package com.ytrue.orm.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author ytrue
 * @date 2022/8/30 20:02
 * @description HashMapTypeHandler
 */
public class HashMapTypeHandler extends BaseTypeHandler<HashMap<String, String>> {
    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, HashMap<String, String> parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.get("name"));
    }

    @Override
    protected HashMap<String, String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return null;
    }
}
