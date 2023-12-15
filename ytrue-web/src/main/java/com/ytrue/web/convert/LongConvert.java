package com.ytrue.web.convert;


/**
 * @author ytrue
 * @date 2023-12-15 11:30
 * @description LongConvert
 */
public class LongConvert extends Convert<Long>{


    public LongConvert(Class<Long> type) {
        super(type);
    }

    @Override
    public Object convert(Object arg) throws Exception {
        return defaultConvert(arg.toString());
    }
}
