package com.ytrue.web.convert;


/**
 * @author ytrue
 * @date 2023-12-15 11:30
 * @description BooleanConvert
 */
public class BooleanConvert extends Convert<Boolean>{

    public BooleanConvert(Class<Boolean> type) {
        super(type);
    }

    @Override
    public Object convert(Object arg) throws Exception {

        return defaultConvert(arg.toString());
    }
}
