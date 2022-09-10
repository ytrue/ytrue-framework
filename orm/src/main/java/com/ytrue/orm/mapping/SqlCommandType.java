package com.ytrue.orm.mapping;

/**
 * @author ytrue
 * @date 2022/8/11 16:18
 * @description SQL 指令类型
 */
public enum SqlCommandType {

    /**
     * 未知
     */
    UNKNOWN,
    /**
     * 插入
     */
    INSERT,
    /**
     * 更新
     */
    UPDATE,
    /**
     * 删除
     */
    DELETE,
    /**
     * 查找
     */
    SELECT;
}
