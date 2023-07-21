package com.ytrue.rpc.serializar;

import com.ytrue.rpc.protocol.Protocol;

/**
 * @author ytrue
 * @date 2023-05-19 14:27
 * @description Serializer
 */
public interface Serializer {

    /**
     * 序列化
     *
     * @param protocol
     * @return
     * @throws Exception
     */
    byte[] encode(Protocol protocol) throws Exception;

    /**
     * 反序列化
     *
     * @param bytes
     * @return
     * @throws Exception
     */
    public Protocol decode(byte[] bytes) throws Exception;

}
