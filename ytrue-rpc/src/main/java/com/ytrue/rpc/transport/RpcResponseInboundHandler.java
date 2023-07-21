package com.ytrue.rpc.transport;

import com.ytrue.rpc.future.SyncWriteFuture;
import com.ytrue.rpc.future.SyncWriteMap;
import com.ytrue.rpc.protocol.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author ytrue
 * @date 2023-05-20 14:21
 * @description RpcResponseInboundHandler
 */
public class RpcResponseInboundHandler extends SimpleChannelInboundHandler<RpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse) throws Exception {
        // 获取id
        String requestId = rpcResponse.getRequestId();

        // 去对于的map获取
        SyncWriteFuture future = (SyncWriteFuture) SyncWriteMap.syncKey.get(requestId);
        if (future != null) {
            future.setResponse(rpcResponse);
        }
    }
}
