package com.ytrue.orm.reflection.wrapper;

import com.ytrue.orm.reflection.MetaObject;

/**
 * @author ytrue
 * @date 2022/8/19 14:36
 * @description 默认对象包装工厂
 */
public class DefaultObjectWrapperFactory implements ObjectWrapperFactory {
    @Override
    public boolean hasWrapperFor(Object object) {
        return false;
    }

    @Override
    public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
        throw new RuntimeException("The DefaultObjectWrapperFactory should never be called to provide an ObjectWrapper.");

    }
}
