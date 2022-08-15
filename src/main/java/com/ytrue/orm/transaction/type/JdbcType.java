package com.ytrue.orm.transaction.type;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2022/8/15 15:21
 * @description JDBC类型枚举
 */
public enum JdbcType {

    /**
     *JDBC类型枚举
     */
    INTEGER(Types.INTEGER),
    FLOAT(Types.FLOAT),
    DOUBLE(Types.DOUBLE),
    DECIMAL(Types.DECIMAL),
    VARCHAR(Types.VARCHAR),
    TIMESTAMP(Types.TIMESTAMP);

    public final int TYPE_CODE;

    private static Map<Integer, JdbcType> codeLookup = new HashMap<>();

    // 就将数字对应的枚举型放入 HashMap
    static {
        for (JdbcType type : JdbcType.values()) {
            codeLookup.put(type.TYPE_CODE, type);
        }
    }

    JdbcType(int code) {
        this.TYPE_CODE = code;
    }

    /**
     * 根据id获取 JdbcType
     * @param code
     * @return
     */
    public static JdbcType forCode(int code) {
        return codeLookup.get(code);
    }

}
