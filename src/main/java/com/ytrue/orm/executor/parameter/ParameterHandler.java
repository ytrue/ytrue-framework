package com.ytrue.orm.executor.parameter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author ytrue
 * @date 2022/8/24 14:54
 * @description 参数处理器
 */
public interface ParameterHandler {

    /**
     * @return
     */
    Object getParameterObject();

    /**
     * @param ps
     * @throws SQLException
     */
    void setParameters(PreparedStatement ps) throws SQLException;
}
