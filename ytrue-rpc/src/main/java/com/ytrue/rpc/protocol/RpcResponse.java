package com.ytrue.rpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author ytrue
 * @date 2023-05-19 14:37
 * @description 响应参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RpcResponse implements Protocol {

    private String requestId;

    /**
     * 结果
     */
    private Object resultValue;

    /**
     * 异常
     */
    private Exception exception;
}
