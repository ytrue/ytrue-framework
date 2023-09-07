package com.ytrue.gateway.core.socket.handlers;

import com.ytrue.gateway.core.bind.IGenericReference;
import com.ytrue.gateway.core.executor.result.SessionResult;
import com.ytrue.gateway.core.session.GatewaySession;
import com.ytrue.gateway.core.session.defaults.DefaultGatewaySessionFactory;
import com.ytrue.gateway.core.socket.BaseHandler;
import com.ytrue.gateway.core.socket.agreement.AgreementConstants;
import com.ytrue.gateway.core.socket.agreement.GatewayResultMessage;
import com.ytrue.gateway.core.socket.agreement.RequestParser;
import com.ytrue.gateway.core.socket.agreement.ResponseParser;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author ytrue
 * @date 2023-09-07 14:22
 * @description 协议数据处理
 */
public class ProtocolDataHandler extends BaseHandler<FullHttpRequest> {

    private final Logger logger = LoggerFactory.getLogger(ProtocolDataHandler.class);

    private final DefaultGatewaySessionFactory gatewaySessionFactory;

    public ProtocolDataHandler(DefaultGatewaySessionFactory gatewaySessionFactory) {
        this.gatewaySessionFactory = gatewaySessionFactory;
    }


    @Override
    protected void session(ChannelHandlerContext ctx, Channel channel, FullHttpRequest request) {
        logger.info("网关接收请求【消息】 uri：{} method：{}", request.uri(), request.method());
        try {
            // 1. 解析请求参数
            RequestParser requestParser = new RequestParser(request);
            String uri = requestParser.getUri();
            if (null == uri) {
                return;
            }
            Map<String, Object> args = requestParser.parse();
            logger.info("解析获取请求的参数：{}", args);

            // 2. 调用会话服务
            GatewaySession gatewaySession = gatewaySessionFactory.openSession(uri);
            IGenericReference reference = gatewaySession.getMapper();
            SessionResult result = reference.$invoke(args);

            // 3. 封装返回结果
            DefaultFullHttpResponse response = new ResponseParser().parse(
                    "0000".equals(result.getCode()) ? GatewayResultMessage.buildSuccess(result.getData()) : GatewayResultMessage.buildError(AgreementConstants.ResponseCode._404.getCode(), "网关协议调用失败！"));
            channel.writeAndFlush(response);
        } catch (Exception e) {
            // 4. 封装返回结果
            DefaultFullHttpResponse response = new ResponseParser().parse(GatewayResultMessage.buildError(AgreementConstants.ResponseCode._502.getCode(), "网关协议调用失败！" + e.getMessage()));
            channel.writeAndFlush(response);
        }
    }
}
