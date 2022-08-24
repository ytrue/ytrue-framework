package com.ytrue.orm.mapping;

import com.ytrue.orm.scripting.LanguageDriver;
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
     * 绑定Sql
     */
    private SqlSource sqlSource;

    /**
     * 返回类型
     */
    Class<?> resultType;

    /**
     * 脚本语言驱动
     */
    private LanguageDriver lang;

}
