package com.ytrue.rpc.future;

import com.ytrue.rpc.protocol.RpcResponse;

import java.util.concurrent.Future;

/**
 * @author ytrue
 * @date 2023-05-20 13:51
 * @description WriteFuture
 */
public interface WriteFuture<T> extends Future<T> {

    /**
     * 异常
     *
     * @return
     */
    Throwable cause();

    /**
     * 设置异常
     *
     * @param cause
     */
    void setCause(Throwable cause);

    /**
     * 是否写成功
     *
     * @return
     */
    boolean isWriteSuccess();

    /**
     * 设置写结果
     *
     * @param result
     */
    void setWriteResult(boolean result);

    /**
     * 请求id
     *
     * @return
     */
    String requestId();

    /**
     * 响应
     *
     * @return
     */
    T response();

    /**
     * 设置响应
     *
     * @param response
     */
    void setResponse(RpcResponse response);

    /**
     * 是否超时
     *
     * @return
     */
    boolean isTimeout();
}
