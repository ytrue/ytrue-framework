package com.ytrue.web.convert;

import java.util.Date;


/**
 * @author ytrue
 * @date 2023-12-15 11:30
 * @description DateConvert
 */
public class DateConvert extends Convert<Date>{


    public DateConvert(Class<Date> type) {
        super(type);
    }

    @Override
    public Object convert(Object arg) throws Exception {
        return defaultConvert(arg.toString());
    }
}
