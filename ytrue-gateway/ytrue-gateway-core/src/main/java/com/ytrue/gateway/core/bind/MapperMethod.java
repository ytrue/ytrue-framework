package com.ytrue.gateway.core.bind;

import com.ytrue.gateway.core.mapping.HttpCommandType;
import com.ytrue.gateway.core.session.Configuration;
import com.ytrue.gateway.core.session.GatewaySession;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-06 17:11
 * @description MapperMethod
 */
public class MapperMethod {

    /**
     * methodName
     */
    private String methodName;

    /**
     * 请求类型
     */
    private final HttpCommandType command;

    public MapperMethod(String uri, Method method, Configuration configuration) {
        this.methodName = configuration.getHttpStatement(uri).getMethodName();
        this.command = configuration.getHttpStatement(uri).getHttpCommandType();
    }

    public Object execute(GatewaySession session, Map<String, Object> params) {
        Object result = null;
        switch (command) {
            case GET:
                result = session.get(methodName, params);
                break;
            case POST:
                result = session.post(methodName, params);
                break;
            case PUT:
                break;
            case DELETE:
                break;
            default:
                throw new RuntimeException("Unknown execution method for: " + command);
        }
        return result;
    }
}
