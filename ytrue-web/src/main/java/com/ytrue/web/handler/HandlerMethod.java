package com.ytrue.web.handler;

import com.ytrue.web.annotation.RequestMethod;
import com.ytrue.web.convert.ConvertHandler;
import org.springframework.core.MethodParameter;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author ytrue
 * @date 2023-12-15 10:12
 * @description HandlerMethod
 */
public class HandlerMethod {

    /**
     * 对象
     */
    protected Object bean;

    /**
     * class
     */
    protected Class type;

    /**
     * 方法
     */
    protected Method method;

    /**
     * http url
     */
    protected String path;


    /**
     * 请求方式 get or post ...
     */
    protected RequestMethod[] requestMethods = new RequestMethod[0];

    /**
     * 方法参数
     */
    protected MethodParameter[] parameters = new MethodParameter[0];


    /**
     * 异常处理
     */
    private Map<Class, ExceptionHandlerMethod> exceptionHandlerMethodMap = new HashMap<>();

    /**
     * 解析器
     */
    private Map<Class, ConvertHandler> convertHandlerMap = new HashMap<>();


    public HandlerMethod() {
    }

    public HandlerMethod(Object bean, Method method) {
        this.bean = bean;
        this.type = bean.getClass();
        this.method = method;
        // 获取方法的参数
        final Parameter[] parameters = method.getParameters();
        MethodParameter[] methodParameters = new MethodParameter[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            // 创建MethodParameter，加入数组
            methodParameters[i] = new MethodParameter(method, i);
        }
        this.parameters = methodParameters;
    }


    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    /**
     * 如果为空，则设置所有请求类型
     *
     * @param requestMethods
     */
    public void setRequestMethods(RequestMethod[] requestMethods) {
        if (ObjectUtils.isEmpty(requestMethods)) {
            requestMethods = RequestMethod.values();
        }
        this.requestMethods = requestMethods;
    }

    public MethodParameter[] getParameters() {
        return parameters;
    }

    public RequestMethod[] getRequestMethods() {
        return requestMethods;
    }

    public Method getMethod() {
        return method;
    }

    public Object getBean() {
        return bean;
    }

    public void setExceptionHandlerMethodMap(Map<Class, ExceptionHandlerMethod> exceptionHandlerMethodMap) {
        this.exceptionHandlerMethodMap = exceptionHandlerMethodMap;
    }

    public Map<Class, ExceptionHandlerMethod> getExceptionHandlerMethodMap() {
        return exceptionHandlerMethodMap;
    }

    public void setConvertHandlerMap(Map<Class, ConvertHandler> convertHandlerMap) {
        this.convertHandlerMap = convertHandlerMap;
    }

    public Map<Class, ConvertHandler> getConvertHandlerMap() {
        return convertHandlerMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HandlerMethod that = (HandlerMethod) o;
        return Objects.equals(path, that.path) &&
               Arrays.equals(requestMethods, that.requestMethods);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(path);
        result = 31 * result + Arrays.hashCode(requestMethods);
        return result;
    }

    @Override
    public String toString() {
        return "HandlerMethod{" +
               "bean=" + bean +
               ", type=" + type +
               ", method=" + method +
               ", path='" + path + '\'' +
               ", requestMethods=" + Arrays.toString(requestMethods) +
               ", parameters=" + Arrays.toString(parameters) +
               '}';
    }
}
