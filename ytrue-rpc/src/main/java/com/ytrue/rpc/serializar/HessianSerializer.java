package com.ytrue.rpc.serializar;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.ytrue.rpc.protocol.Protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author ytrue
 * @date 2023-05-19 14:29
 * @description HessianSerializer
 */
public class HessianSerializer implements Serializer {
    @Override
    public byte[] encode(Protocol protocol) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(outputStream);
        hessian2Output.writeObject(protocol);
        hessian2Output.flush();
        return outputStream.toByteArray();
    }

    @Override
    public Protocol decode(byte[] bytes) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        Hessian2Input hessian2Input = new Hessian2Input(inputStream);
        return (Protocol) hessian2Input.readObject();
    }
}
