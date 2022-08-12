package com.ytrue.orm.mapping;

import com.ytrue.orm.session.Configuration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author ytrue
 * @date 2022/8/11 16:17
 * @description MappedStatement维护一条<select | update | delete | insert>节点的封装。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MappedStatement {

    /**
     * 配置文件
     */
    private Configuration configuration;

    /**
     * Dao的类名加方法名称
     */

    private String id;
    /**
     * sql 类型
     */
    private SqlCommandType sqlCommandType;

    /**
     * 参数类型
     */
    private String parameterType;

    /**
     * 返回类型
     */
    private String resultType;

    /**
     * sql语句
     */
    private String sql;

    /**
     * Dao方法的参数
     */
    private Map<Integer, String> parameter;
}
