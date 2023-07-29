package com.ytrue.netty.channel;

import com.ytrue.netty.util.Attribute;
import com.ytrue.netty.util.AttributeKey;
import com.ytrue.netty.util.AttributeMap;
import com.ytrue.netty.util.concurrent.EventExecutor;

/**
 * @author ytrue
 * @date 2023/7/29 10:46
 * @description 该类是ChannelPipeline中包装handler的类，里面封装着ChannelHandler，和每一个handler的上下文信息
 * 而正是一个个ChannelHandlerContext对象，构成了ChannelPipeline中责任链的链表，链表的每一个节点都是ChannelHandlerContext
 * 对象，每一个ChannelHandlerContext对象里都有一个handler。这个接口也继承了出站入站方法，想一想，这个接口的继承接口，为什么要继承入站出站
 * 的接口呢？
 * 同时也应该注意到该接口继承了AttributeMap接口，这说明ChannelHandlerContext的实现类本身也是一个map，那么用户存储在该实现类中的
 * 参数，在某些类中应该也是可以获得的。
 */
public interface ChannelHandlerContext extends AttributeMap, ChannelInboundInvoker, ChannelOutboundInvoker {

    /**
     * 获取当前的 Channel 对象。
     *
     * @return
     */
    Channel channel();

    /**
     * 获取当前的 EventExecutor 对象，用于执行任务
     *
     * @return
     */
    EventExecutor executor();

    /**
     * 获取当前 ChannelHandler 的名称。
     *
     * @return
     */
    String name();

    /**
     * 获取当前 ChannelHandler 对象。
     *
     * @return
     */
    ChannelHandler handler();

    /**
     * 是否移除
     *
     * @return
     */
    boolean isRemoved();

    /**
     * 获取当前 ChannelHandler 所属的 ChannelPipeline 对象。
     *
     * @return
     */
    ChannelPipeline pipeline();

    @Override
    ChannelHandlerContext fireChannelRegistered();

    @Override
    ChannelHandlerContext fireChannelUnregistered();

    @Override
    ChannelHandlerContext fireChannelActive();

    @Override
    ChannelHandlerContext fireChannelInactive();

    @Override
    ChannelHandlerContext fireExceptionCaught(Throwable cause);

    @Override
    ChannelHandlerContext fireUserEventTriggered(Object evt);

    @Override
    ChannelHandlerContext fireChannelRead(Object msg);

    @Override
    ChannelHandlerContext fireChannelReadComplete();

    @Override
    ChannelHandlerContext fireChannelWritabilityChanged();

    // 下面两个是ChannelOutboundInvoker接口的方法
    @Override
    ChannelHandlerContext read();

    @Override
    ChannelHandlerContext flush();


    @Deprecated
    @Override
    <T> Attribute<T> attr(AttributeKey<T> key);


    @Deprecated
    @Override
    <T> boolean hasAttr(AttributeKey<T> key);
}
