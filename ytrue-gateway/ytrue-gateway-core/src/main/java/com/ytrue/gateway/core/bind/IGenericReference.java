package com.ytrue.gateway.core.bind;

import com.ytrue.gateway.core.executor.result.SessionResult;

import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-06 14:18
 * @description 统一泛化调用接口
 */
public interface IGenericReference {


    /**
     * 调用
     *
     * @param params
     * @return
     */
    SessionResult $invoke(Map<String, Object> params);
}
