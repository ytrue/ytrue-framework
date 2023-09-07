package com.ytrue.gateway.core.executor;


import com.ytrue.gateway.core.executor.result.SessionResult;
import com.ytrue.gateway.core.mapping.HttpStatement;

import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-07 10:49
 * @description Executor
 */
public interface Executor {

    /**
     * 执行
     *
     * @param httpStatement
     * @param params
     * @return
     * @throws Exception
     */
    SessionResult exec(HttpStatement httpStatement, Map<String, Object> params) throws Exception;

}
