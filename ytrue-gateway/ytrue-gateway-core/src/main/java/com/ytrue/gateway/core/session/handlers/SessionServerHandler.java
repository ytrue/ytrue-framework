package com.ytrue.gateway.core.session.handlers;

import com.google.gson.Gson;
import com.ytrue.gateway.core.bind.IGenericReference;
import com.ytrue.gateway.core.session.BaseHandler;
import com.ytrue.gateway.core.session.Configuration;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ytrue
 * @date 2023-09-06 11:18
 * @description SessionServerHandler
 */
public class SessionServerHandler extends BaseHandler<FullHttpRequest> {

    private final Logger logger = LoggerFactory.getLogger(SessionServerHandler.class);


    private final Configuration configuration;

    public SessionServerHandler(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void session(ChannelHandlerContext ctx, final Channel channel, FullHttpRequest request) {
        logger.info("网关接收请求 uri：{} method：{}", request.uri(), request.method());

        // 返回信息处理
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);


        // 服务泛化调用
        IGenericReference reference = configuration.getGenericReference("sayHi");
        String result = reference.$invoke("test") + " " + System.currentTimeMillis();

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
