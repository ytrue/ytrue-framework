package com.ytrue.web.convert;

/**
 * @author ytrue
 * @date 2023-12-15 11:30
 * @description Convert
 */
public abstract class Convert<T> {

    protected Class<T> type;

    public Class<T> getType() {
        return type;
    }

    public Convert(Class<T> type) {
        this.type = type;
    }

    /**
     * 转换
     *
     * @param arg
     * @return
     * @throws Exception
     */
    protected abstract Object convert(Object arg) throws Exception;


    /**
     * 默认转换
     *
     * @param text
     * @return
     * @throws Exception
     */
    protected Object defaultConvert(String text) throws Exception {
        // Long l = Long.class.getConstructor(String.class).newInstance("123");
        // l == 123
        return type.getConstructor(String.class).newInstance(text);
    }

}
