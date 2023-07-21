package com.ytrue.rpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author ytrue
 * @date 2023-05-19 14:37
 * @description 请求参数
 */
@Data
@NoArgsConstructor
@ToString
public class RpcRequest implements Protocol {

    private String requestId;

    /**
     * 目标类
     */
    private Class targetInterface;

    /**
     * 调用方法
     */
    private String methodName;

    /**
     * 方法形参
     */
    private Class<?>[] parameterTypes;

    /**
     * 方法实参
     */
    private Object[] args;

    public RpcRequest(Class targetInterface, String methodName, Class<?>[] parameterTypes, Object[] args) {
        this.targetInterface = targetInterface;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.args = args;
    }
}
