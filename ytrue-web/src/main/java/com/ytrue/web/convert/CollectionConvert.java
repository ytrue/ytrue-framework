package com.ytrue.web.convert;

import java.util.ArrayList;
import java.util.Collection;


/**
 * @author ytrue
 * @date 2023-12-15 11:30
 * @description CollectionConvert
 */
public class CollectionConvert extends Convert<Collection>{

    public CollectionConvert(Class<Collection> type) {
        super(type);
    }

    @Override
    protected Object convert(Object arg) throws Exception {
        return ArrayList.class.getConstructor(Collection.class).newInstance(arg);
    }
}
