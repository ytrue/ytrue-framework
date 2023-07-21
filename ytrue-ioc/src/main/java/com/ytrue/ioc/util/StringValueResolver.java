package com.ytrue.ioc.util;

/**
 * @author ytrue
 * @date 2022/10/18 15:18
 * @description 定义解析字符串接口
 */
public interface StringValueResolver {

    /**
     * 解析字符串内容
     *
     * @param strVal
     * @return
     */
    String resolveStringValue(String strVal);
}
