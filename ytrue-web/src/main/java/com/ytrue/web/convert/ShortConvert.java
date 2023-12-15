package com.ytrue.web.convert;


/**
 * @author ytrue
 * @date 2023-12-15 11:30
 * @description ShortConvert
 */
public class ShortConvert extends Convert<Short>{


    public ShortConvert(Class<Short> type) {
        super(type);
    }

    @Override
    public Object convert(Object arg) throws Exception {
        return defaultConvert(arg.toString());
    }
}
