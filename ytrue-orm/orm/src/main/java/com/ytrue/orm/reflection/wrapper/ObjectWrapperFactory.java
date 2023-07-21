package com.ytrue.orm.reflection.wrapper;

import com.ytrue.orm.reflection.MetaObject;

/**
 * @author ytrue
 * @date 2022/8/19 14:35
 * @description 对象包装工厂
 */
public interface ObjectWrapperFactory {

    /**
     * 判断有没有包装器
     */
    boolean hasWrapperFor(Object object);

    /**
     * 得到包装器
     *
     * @param metaObject
     * @param object
     * @return
     */
    ObjectWrapper getWrapperFor(MetaObject metaObject, Object object);

}
