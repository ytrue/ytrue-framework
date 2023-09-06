package com.ytrue.gateway.core.socket.handlers;

import com.google.gson.Gson;
import com.ytrue.gateway.core.bind.IGenericReference;
import com.ytrue.gateway.core.session.GatewaySession;
import com.ytrue.gateway.core.session.defaults.DefaultGatewaySessionFactory;
import com.ytrue.gateway.core.socket.BaseHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ytrue
 * @date 2023-09-06 11:18
 * @description GatewayServerHandler
 */
public class GatewayServerHandler extends BaseHandler<FullHttpRequest> {

    private final Logger logger = LoggerFactory.getLogger(GatewayServerHandler.class);


    private final DefaultGatewaySessionFactory gatewaySessionFactory;

    public GatewayServerHandler(DefaultGatewaySessionFactory gatewaySessionFactory) {
        this.gatewaySessionFactory = gatewaySessionFactory;
    }


    @Override
    protected void session(ChannelHandlerContext ctx, final Channel channel, FullHttpRequest request) {
        logger.info("网关接收请求 uri：{} method：{}", request.uri(), request.method());


        // 返回信息控制：简单处理
        String uri = request.uri();
        if (uri.equals("/favicon.ico")) return;


        // 服务泛化调用
        GatewaySession gatewaySession = gatewaySessionFactory.openSession();
        IGenericReference reference = gatewaySession.getMapper(uri);
        String result = reference.$invoke("test") + " " + System.currentTimeMillis();


        // 返回信息处理
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

        // 返回信息控制
        Gson gson = new Gson();
        byte[] data = gson.toJson(result).getBytes();
        response.content().writeBytes(data);
        // 头部信息设置
        HttpHeaders heads = response.headers();
        // 返回内容类型
        heads.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + "; charset=UTF-8");
        // 响应体的长度
        heads.add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        // 配置持久连接
        heads.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        // 配置跨域访问
        heads.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        heads.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        heads.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE");
        heads.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

        channel.writeAndFlush(response);
    }

}
