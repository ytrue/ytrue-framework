package com.ytrue.ioc.beans;

/**
 * @author ytrue
 * @date 2022/9/29 09:28
 * @description bean 属性信息
 */
public class PropertyValue {

    /**
     * 名称
     */
    private final String name;

    /**
     * 值
     */
    private final Object value;

    public PropertyValue(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}
