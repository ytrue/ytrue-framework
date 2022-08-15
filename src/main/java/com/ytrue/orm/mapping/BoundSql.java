package com.ytrue.orm.mapping;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * @author ytrue
 * @date 2022/8/15 15:41
 * @description 绑定的SQL, 是从SqlSource而来，将动态内容都处理完成得到的SQL语句字符串，其中包括?,还有绑定的参数
 */
@Getter
@AllArgsConstructor
public class BoundSql {


    /**
     * sql语句
     */
    private String sql;

    /**
     * Dao方法的参数
     */
    private Map<Integer, String> parameterMappings;

    /**
     * 参数类型
     */
    private String parameterType;

    /**
     * 返回类型
     */
    private String resultType;
}
