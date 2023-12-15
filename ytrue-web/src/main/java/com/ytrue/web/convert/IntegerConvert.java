package com.ytrue.web.convert;


/**
 * @author ytrue
 * @date 2023-12-15 11:30
 * @description IntegerConvert
 */
public class IntegerConvert extends Convert<Integer>{



    public IntegerConvert(Class<Integer> type) {
        super(type);
    }

    @Override
    public Object convert(Object arg) throws Exception {
        return defaultConvert(arg.toString());
    }
}
