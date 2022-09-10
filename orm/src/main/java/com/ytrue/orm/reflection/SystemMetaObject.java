package com.ytrue.orm.reflection;

import com.ytrue.orm.reflection.factory.DefaultObjectFactory;
import com.ytrue.orm.reflection.factory.ObjectFactory;
import com.ytrue.orm.reflection.wrapper.DefaultObjectWrapperFactory;
import com.ytrue.orm.reflection.wrapper.ObjectWrapperFactory;

/**
 * @author ytrue
 * @date 2022/8/19 14:56
 * @description 一些系统级别的元对象
 */
public class SystemMetaObject {

    public static final ObjectFactory DEFAULT_OBJECT_FACTORY = new DefaultObjectFactory();
    public static final ObjectWrapperFactory DEFAULT_OBJECT_WRAPPER_FACTORY = new DefaultObjectWrapperFactory();
    public static final MetaObject NULL_META_OBJECT = MetaObject.forObject(NullObject.class, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY);


    /**
     * 空对象
     */
    private static class NullObject {
    }

    public static MetaObject forObject(Object object) {
        return MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY);
    }
}
