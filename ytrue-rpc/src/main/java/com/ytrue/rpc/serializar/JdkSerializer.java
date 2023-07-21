package com.ytrue.rpc.serializar;

import com.ytrue.rpc.protocol.Protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author ytrue
 * @date 2023-05-19 14:29
 * @description JdkSerializar
 */
public class JdkSerializer implements Serializer {
    @Override
    public byte[] encode(Protocol protocol) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(protocol);
        return outputStream.toByteArray();
    }

    @Override
    public Protocol decode(byte[] bytes) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        return (Protocol) objectInputStream.readObject();
    }
}
