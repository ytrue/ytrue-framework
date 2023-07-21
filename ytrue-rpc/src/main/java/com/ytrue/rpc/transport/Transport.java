package com.ytrue.rpc.transport;

import com.ytrue.rpc.protocol.RpcRequest;
import com.ytrue.rpc.protocol.RpcResponse;
import com.ytrue.rpc.register.HostAndPort;

/**
 * @author ytrue
 * @date 2023-05-19 19:41
 * @description Transport
 */
public interface Transport {

    /**
     * 调用
     *
     * @param hostAndPort
     * @param request
     * @return
     * @throws Exception
     */
    public RpcResponse invoke(HostAndPort hostAndPort, RpcRequest request) throws Exception;

    /**
     * 关闭客户端
     */
    public void close();
}
