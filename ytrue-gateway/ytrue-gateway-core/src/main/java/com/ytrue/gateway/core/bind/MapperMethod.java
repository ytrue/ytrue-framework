package com.ytrue.gateway.core.bind;

import com.ytrue.gateway.core.mapping.HttpCommandType;
import com.ytrue.gateway.core.session.Configuration;
import com.ytrue.gateway.core.session.GatewaySession;

import java.lang.reflect.Method;

/**
 * @author ytrue
 * @date 2023-09-06 17:11
 * @description MapperMethod
 */
public class MapperMethod {

    /**
     * url
     */
    private String uri;

    /**
     * 请求类型
     */
    private final HttpCommandType command;

    public MapperMethod(String uri, Method method, Configuration configuration) {
        this.uri = uri;
        this.command = configuration.getHttpStatement(uri).getHttpCommandType();
    }

    public Object execute(GatewaySession session, Object args) {
        Object result = null;
        switch (command) {
            case GET:
                result = session.get(uri, args);
                break;
            case POST:
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
