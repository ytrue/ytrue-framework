package com.ytrue.orm.mapping;

import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.type.JdbcType;
import com.ytrue.orm.type.TypeHandler;
import lombok.Data;

/**
 * @author ytrue
 * @date 2022/8/25 14:59
 * @description 结果映射
 */
@Data
public class ResultMapping {

    private Configuration configuration;

    /**
     * 属性
     */
    private String property;

    /**
     * 字段
     */
    private String column;

    /**
     * java类型
     */
    private Class<?> javaType;

    /**
     * jdbc类型
     */
    private JdbcType jdbcType;

    /**
     * 类型处理器
     */
    private TypeHandler<?> typeHandler;

}
