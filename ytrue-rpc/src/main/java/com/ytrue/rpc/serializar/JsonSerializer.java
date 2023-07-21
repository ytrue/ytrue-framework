package com.ytrue.rpc.serializar;

import com.google.gson.Gson;
import com.ytrue.rpc.protocol.Protocol;

/**
 * @author ytrue
 * @date 2023-05-19 14:29
 * @description JsonSerializer
 */
public class JsonSerializer implements Serializer {

    private final Gson gson = new Gson();

    @Override
    public byte[] encode(Protocol protocol) throws Exception {
        return gson.toJson(protocol).getBytes();
    }

    @Override
    public Protocol decode(byte[] bytes) throws Exception {
        return gson.fromJson(new String(bytes), Protocol.class);
    }
}
