package com.ytrue.ioc.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2022/9/29 09:33
 * @description PropertyValues
 */
public class PropertyValues {

    /**
     * 一组bean属性
     */
    private final List<PropertyValue> propertyValueList = new ArrayList<>();

    /**
     * 添加
     *
     * @param pv
     */
    public void addPropertyValue(PropertyValue pv) {
        this.propertyValueList.add(pv);
    }

    /**
     * 获取全部
     *
     * @return
     */
    public PropertyValue[] getPropertyValues() {
        // 转换成数组
        return this.propertyValueList.toArray(new PropertyValue[0]);
    }

    /**
     * 根据name获取
     *
     * @param propertyName
     * @return
     */
    public PropertyValue getPropertyValue(String propertyName) {
        for (PropertyValue pv : this.propertyValueList) {
            if (pv.getName().equals(propertyName)) {
                return pv;
            }
        }
        return null;
    }

}
