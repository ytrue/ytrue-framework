package com.ytrue.web.convert;

import java.util.HashMap;
import java.util.Map;


/**
 * @author ytrue
 * @date 2023-12-15 11:30
 * @description MapConvert
 */
public class MapConvert extends Convert<HashMap>{

    public MapConvert(Class<HashMap> type) {
        super(type);
    }

    @Override
    protected Object convert(Object arg) throws Exception {
        return this.type.getConstructor(Map.class).newInstance(arg);
    }
}
