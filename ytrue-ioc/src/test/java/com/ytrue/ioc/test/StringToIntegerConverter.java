package com.ytrue.ioc.test;


import com.ytrue.ioc.core.convert.converter.Converter;

public class StringToIntegerConverter implements Converter<String, Integer> {

    @Override
    public Integer convert(String source) {
        return Integer.valueOf(source);
    }

}
