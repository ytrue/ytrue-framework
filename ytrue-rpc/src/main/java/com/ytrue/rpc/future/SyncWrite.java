package com.ytrue.rpc.future;

import com.ytrue.rpc.protocol.RpcRequest;
import com.ytrue.rpc.protocol.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * @author ytrue
 * @date 2023-05-20 13:59
 * @description SyncWrite
 */
public class SyncWrite {

    /**
     * 异步写
     *
     * @param channel
     * @param request
     * @param timeout
     * @return
     * @throws Exception
     */
    public RpcResponse writeAndSync(final Channel channel, final RpcRequest request, final long timeout) throws Exception {
        // 校验
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        if (request == null) {
            throw new NullPointerException("request");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout <= 0");
        }

        // 生成id
        String requestId = UUID.randomUUID().toString();
        request.setRequestId(requestId);

        WriteFuture<RpcResponse> future = new SyncWriteFuture(request.getRequestId());
        // 存储
        SyncWriteMap.syncKey.put(request.getRequestId(), future);

        // 执行
        RpcResponse response = doWriteAndSync(channel, request, timeout, future);

        // 删除
        SyncWriteMap.syncKey.remove(request.getRequestId());
        return response;
    }

    /**
     * 发送
     *
     * @param channel
     * @param request
     * @param timeout
     * @param writeFuture
     * @return
     * @throws Exception
     */
    private RpcResponse doWriteAndSync(final Channel channel, final RpcRequest request, final long timeout, final WriteFuture<RpcResponse> writeFuture) throws Exception {
        // 发送
        channel.writeAndFlush(request).addListener(new GenericFutureListener() {
            @Override
            public void operationComplete(Future future) {
                // 设置结果
                writeFuture.setWriteResult(future.isSuccess());
                writeFuture.setCause(future.cause());
                //失败移除
                if (!writeFuture.isWriteSuccess()) {
                    SyncWriteMap.syncKey.remove(writeFuture.requestId());
                }
            }
        });

        // 获取结果
        RpcResponse response = writeFuture.get(timeout, TimeUnit.MILLISECONDS);

        if (response == null) {
            if (writeFuture.isTimeout()) {
                throw new TimeoutException();
            } else {
                // write exception
                throw new Exception(writeFuture.cause());
            }
        }
        return response;
    }
}
