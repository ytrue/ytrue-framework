package com.ytrue.web.convert;


/**
 * @author ytrue
 * @date 2023-12-15 11:30
 * @description ByteConvert
 */
public class ByteConvert extends Convert<Byte> {


    public ByteConvert(Class<Byte> type) {
        super(type);
    }

    @Override
    public Object convert(Object arg) throws Exception {
        return defaultConvert(arg.toString());
    }
}
