package com.ytrue.web.convert;

import java.util.Collection;
import java.util.HashSet;


/**
 * @author ytrue
 * @date 2023-12-15 11:30
 * @description SetConvert
 */
public class SetConvert extends Convert<HashSet>{

    public SetConvert(Class<HashSet> type) {
        super(type);
    }

    @Override
    protected Object convert(Object arg) throws Exception {
        return this.type.getConstructor(Collection.class).newInstance(arg);
    }
}
