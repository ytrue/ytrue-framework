package com.ytrue.orm.mapping;

import com.ytrue.orm.session.Configuration;
import com.ytrue.orm.type.JdbcType;
import lombok.Builder;
import lombok.Data;

/**
 * @author ytrue
 * @date 2022/8/23 14:21
 * @description 参数映射 #{property,javaType=int,jdbcType=NUMERIC}
 */
@Data
@Builder
public class ParameterMapping {

    private Configuration configuration;

    /**
     * #{name} 里面的 name
     */
    private String property;

    /**
     * javaType = int  对应的java类型
     */
    private Class<?> javaType;

    /**
     * jdbcType=NUMERIC  数据类型
     */
    private JdbcType jdbcType;
}
