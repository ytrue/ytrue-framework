package com.ytrue.web.convert;

import java.util.ArrayList;
import java.util.Collection;


/**
 * @author ytrue
 * @date 2023-12-15 11:30
 * @description ListConvert
 */
public class ListConvert extends Convert<ArrayList>{

    public ListConvert(Class<ArrayList> type) {
        super(type);
    }

    @Override
    protected Object convert(Object arg) throws Exception {
        return this.type.getConstructor(Collection.class).newInstance(arg);

    }
}
