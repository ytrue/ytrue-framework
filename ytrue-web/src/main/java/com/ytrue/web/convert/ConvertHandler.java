package com.ytrue.web.convert;

import com.ytrue.web.handler.HandlerMethod;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2023-12-15 10:39
 * @description 转换器处理
 */
public class ConvertHandler extends HandlerMethod {

    public ConvertHandler(Object bean, Method method) {
        super(bean, method);
    }

    /**
     * 转换
     *
     * @param arg
     * @return
     * @throws Exception
     */
    public Object convert(Object arg) throws Exception {
        // 判断初始是否为空
        if (ObjectUtils.isEmpty(arg)) {
            return null;
        }
        // 反射调用方法
        return this.getMethod().invoke(this.getBean(), arg);
    }

}
