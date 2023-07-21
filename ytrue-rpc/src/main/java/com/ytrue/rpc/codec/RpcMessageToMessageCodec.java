package com.ytrue.rpc.codec;

import com.ytrue.rpc.protocol.Protocol;
import com.ytrue.rpc.serializar.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-05-19 14:40
 * @description 编解码器
 */
@Slf4j
public class RpcMessageToMessageCodec extends MessageToMessageCodec<ByteBuf, Protocol> {

    /**
     * 序列化
     */
    private final Serializer serializer;

    public RpcMessageToMessageCodec(Serializer serializer) {
        this.serializer = serializer;
    }

    /**
     * 编码
     *
     * @param channelHandlerContext
     * @param protocol
     * @param list
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Protocol protocol, List<Object> list) throws Exception {
        log.debug("RpcMessageToMessageCodec 编码器运行了....");
        ByteBufAllocator alloc = channelHandlerContext.alloc();
        ByteBuf byteBuf = alloc.buffer();

        try {
            // 数据
            byte[] bytes = serializer.encode(protocol);

            //1幻术 9个字节
            byteBuf.writeBytes(Protocol.MAGIC_NUM.getBytes(StandardCharsets.UTF_8));
            //2设置协议版本 1个字节
            byteBuf.writeByte(Protocol.PROTOCOL_VERSION);
            //封帧的解码器 数据大小是多少
            byteBuf.writeInt(bytes.length);
            byteBuf.writeBytes(bytes);

            // 写出
            list.add(byteBuf);
        } catch (Exception e) {
            log.error("RpcMessageToMessageCodec 编码器出现了异常", e);
        }
    }

    /**
     * 解码
     *
     * @param channelHandlerContext
     * @param byteBuf
     * @param list
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {

        //获取魔术 进行魔术的对比
        CharSequence charSequence = byteBuf.readCharSequence(9, StandardCharsets.UTF_8);
        if (!charSequence.equals(Protocol.MAGIC_NUM)) {
            throw new RuntimeException("MagicNumber error...");
        }

        byte protocolVersion = byteBuf.readByte();
        if (Protocol.PROTOCOL_VERSION != protocolVersion) {
            throw new RuntimeException("ProtocolVersion Error...");
        }

        //1. ByteBuf msg ---> byte[]
        //byte[]长度
        int protocolLength = byteBuf.readInt();
        byte[] bytes = new byte[protocolLength];

        // 读取数据
        byteBuf.readBytes(bytes);

        //2. 反序列化操作
        Protocol protocol = serializer.decode(bytes);

        //3 写出去
        list.add(protocol);
    }
}
