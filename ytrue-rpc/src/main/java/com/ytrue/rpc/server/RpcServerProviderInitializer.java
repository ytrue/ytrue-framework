package com.ytrue.rpc.server;

import com.ytrue.rpc.codec.RpcMessageToMessageCodec;
import com.ytrue.rpc.serializar.HessianSerializer;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LoggingHandler;

import java.util.Map;

/**
 * @author ytrue
 * @date 2023-05-19 15:07
 * @description RpcServerProviderInitializer
 */
class RpcServerProviderInitializer extends ChannelInitializer<NioSocketChannel> {

    private final EventLoopGroup eventLoopGroupHandler;

    private final EventLoopGroup eventLoopGroupService;

    private final Map<String, Object> exposeBean;

    public RpcServerProviderInitializer(EventLoopGroup eventLoopGroupHandler, EventLoopGroup eventLoopGroupService, Map<String, Object> exposeBean) {
        this.eventLoopGroupHandler = eventLoopGroupHandler;
        this.eventLoopGroupService = eventLoopGroupService;
        this.exposeBean = exposeBean;
    }

    @Override
    protected void initChannel(NioSocketChannel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();
        //1. 封祯 LengthFieldBaseFrameDecoder eventLoopGroupHandler
        pipeline.addLast(this.eventLoopGroupHandler, new LengthFieldBasedFrameDecoder(1024, 10, 4, 0, 0));
        //2. LoggingHandler                  eventLoopGroupHandler
        pipeline.addLast(this.eventLoopGroupHandler, new LoggingHandler());
        //3. 编解码 RPCMessageToMessageCodec  eventLoopGroupService
        pipeline.addLast(this.eventLoopGroupService, new RpcMessageToMessageCodec(new HessianSerializer()));
        //4。 RPC功能的调用 eventLoopGroupService
        pipeline.addLast(this.eventLoopGroupService, new RpcRequestInboundHandler(exposeBean));
    }
}
