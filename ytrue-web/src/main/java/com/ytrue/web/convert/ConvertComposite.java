package com.ytrue.web.convert;

import com.ytrue.web.excpetion.NotFoundException;
import com.ytrue.web.handler.HandlerMethod;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-12-15 11:40
 * @description ConvertComposite
 */
public class ConvertComposite {
    private Map<Class, ConvertHandler> convertMap = new HashMap<>();


    public void addConvertMap(Map<Class, ConvertHandler> convertMap) {
        this.convertMap.putAll(convertMap);
    }

    /**
     * 当前是全局的类型转换器
     *
     * @param handlerMethod
     * @param parameterType
     * @param result
     * @return
     * @throws Exception
     */
    public Object convert(HandlerMethod handlerMethod, Class<?> parameterType, Object result) throws Exception {

        // 先执行局部的
        final Map<Class, ConvertHandler> convertHandlerMap = handlerMethod.getConvertHandlerMap();

        if (!ObjectUtils.isEmpty(convertHandlerMap)) {
            final ConvertHandler convertHandler = convertHandlerMap.get(parameterType);
            if (convertHandler != null) {
                return convertHandler.convert(result);
            }
        }

        // 全局的
        if (convertMap.containsKey(parameterType)) {
            final ConvertHandler convertHandler = convertMap.get(parameterType);
            try {
                return convertHandler.convert(result);
            } catch (Exception e) {
                // 类型转换异常
                e.printStackTrace();
            }
        }
        // 没找到
        throw new NotFoundException(parameterType.getName() + "没有该参数类型的类型转换器");
    }
}
