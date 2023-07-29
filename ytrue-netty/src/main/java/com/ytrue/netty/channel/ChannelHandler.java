package com.ytrue.netty.channel;

import java.lang.annotation.*;

/**
 * @author ytrue
 * @date 2023/7/29 10:45
 * @description ChannelHandler是ChannelPipeline的核心，对每一次接收到的数据的处理，靠的就是ChannelHandler
 */
public interface ChannelHandler {

    /**
     * 当 ChannelHandler 被添加到 ChannelPipeline 中时，会触发该方法的调用
     *
     * @param ctx
     * @throws Exception
     */
    void handlerAdded(ChannelHandlerContext ctx) throws Exception;


    /**
     * 当 ChannelHandler 从ChannelPipeline移除到，会触发该方法的调用
     *
     * @param ctx
     * @throws Exception
     */
    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;


    /**
     * 当处理过程中发生异常时，会触发该方法的调用
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Deprecated
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;

    /**
     * :ChannelHandler是否可以公用的注解，这要考虑到并发问题。
     */
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {

    }
}
