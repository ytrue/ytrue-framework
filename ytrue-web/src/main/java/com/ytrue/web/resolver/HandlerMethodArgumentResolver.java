package com.ytrue.web.resolver;

import com.ytrue.web.convert.ConvertComposite;
import com.ytrue.web.handler.HandlerMethod;
import com.ytrue.web.support.WebServletRequest;
import org.springframework.core.MethodParameter;

/**
 * @author ytrue
 * @date 2023-12-15 13:30
 * @description HandlerMethodArgumentResolver
 */
public interface HandlerMethodArgumentResolver {
    /**
     * 当前参数是否支持当前的请求中携带的数据
     *
     * @param parameter
     * @return
     */
    boolean supportsParameter(MethodParameter parameter);

    /**
     * 解析参数
     *
     * @param parameter
     * @param handlerMethod
     * @param webServletRequest
     * @param convertComposite
     * @return
     * @throws Exception
     */
    Object resolveArgument(MethodParameter parameter, HandlerMethod handlerMethod, WebServletRequest webServletRequest, ConvertComposite convertComposite) throws Exception;

}
