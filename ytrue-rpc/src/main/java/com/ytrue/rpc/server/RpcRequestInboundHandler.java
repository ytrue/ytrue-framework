package com.ytrue.rpc.server;

import com.ytrue.rpc.protocol.RpcRequest;
import com.ytrue.rpc.protocol.RpcResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-05-19 16:50
 * @description RpcRequestInboundHandler
 */
@Slf4j
public class RpcRequestInboundHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private final Map<String, Object> exposeBean;

    public RpcRequestInboundHandler(Map<String, Object> exposeBean) {
        this.exposeBean = exposeBean;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest rpcRequest) throws Exception {
        RpcResponse response = executeTargetObject(rpcRequest);
        //进行响应
        ChannelFuture channelFuture = ctx.writeAndFlush(response);

        //关闭连接
        channelFuture.addListener(ChannelFutureListener.CLOSE);
        channelFuture.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    /**
     * 具体方法调用
     *
     * @param rpcRequest
     * @return
     * @throws NoSuchMethodException
     */
    private RpcResponse executeTargetObject(RpcRequest rpcRequest) throws NoSuchMethodException {
        log.debug("executeTargetObject {} ", rpcRequest);

        //获取接口信息
        Class<?> targetInterface = rpcRequest.getTargetInterface();
        //获取接口的实现类的对象
        Object nativeObj = exposeBean.get(targetInterface.getName());
        //获取接口中需要调用的方法 方法名，方法形参类型
        Method method = targetInterface.getDeclaredMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());

        //进行方法的调用
        RpcResponse result = new RpcResponse();
        try {
            Object ret = method.invoke(nativeObj, rpcRequest.getArgs());
            log.debug("method invoke returnValue is {} ", ret);
            result.setResultValue(ret);
        } catch (Exception e) {
            log.error("method invoke error", e);
            result.setException(e);
        }
        // 设置id
        result.setRequestId(rpcRequest.getRequestId());
        return result;
    }
}
