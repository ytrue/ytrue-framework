package com.ytrue.netty.channel;

import com.ytrue.netty.util.concurrent.EventExecutorGroup;

import java.util.List;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023/7/29 10:57
 * @description 可以看到ChannelPipeline接口继承了入站方法的接口和出站方法的接口，而我们知道ChannelPipeline只是个通道，或者说管道，
 * 在该通道中真正处理数据的是ChannelHandler，由此可以想到，该接口这样定义，肯定是在ChannelPipeline调用入站或者出站方法，而由每一个handler
 * 去处理，可想而知，ChannelHandler肯定也会通过一些方式，继承接口或者继承某些类，从而可以调用出站或入站方法
 * 所以在这个接口中，我们的关注重点肯定要放在前面这些addxx方法上，这些就是把ChannelHandler添加进ChannelPipeline的重要方法
 * 当然，我们不会每个方法都去实现，讲解完核心的方法后去看源码，会很容易理解其他的方法
 */
public interface ChannelPipeline extends ChannelInboundInvoker, ChannelOutboundInvoker, Iterable<Map.Entry<String, ChannelHandler>> {

    /**
     * 向管道的开头添加一个或多个 ChannelHandler。这些 ChannelHandler 将按照添加的顺序被调用。
     *
     * @param name
     * @param handler
     * @return
     */
    ChannelPipeline addFirst(String name, ChannelHandler handler);

    /**
     * 向管道的开头添加一个或多个 ChannelHandler。这些 ChannelHandler 将按照添加的顺序被调用。
     *
     * @param group
     * @param name
     * @param handler
     * @return
     */
    ChannelPipeline addFirst(EventExecutorGroup group, String name, ChannelHandler handler);

    /**
     * 向管道的开头添加一个或多个 ChannelHandler。这些 ChannelHandler 将按照添加的顺序被调用。
     *
     * @param handlers
     * @return
     */
    ChannelPipeline addFirst(ChannelHandler... handlers);

    /**
     * 向管道的开头添加一个或多个 ChannelHandler。这些 ChannelHandler 将按照添加的顺序被调用。
     *
     * @param group
     * @param handlers
     * @return
     */
    ChannelPipeline addFirst(EventExecutorGroup group, ChannelHandler... handlers);


    /**
     * 向管道的末尾添加一个或多个 ChannelHandler。这些 ChannelHandler 将按照添加的顺序被调用。
     *
     * @param name
     * @param handler
     * @return
     */
    ChannelPipeline addLast(String name, ChannelHandler handler);

    /**
     * 向管道的末尾添加一个或多个 ChannelHandler。这些 ChannelHandler 将按照添加的顺序被调用。
     *
     * @param group
     * @param name
     * @param handler
     * @return
     */
    ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler);


    /**
     * 向管道的末尾添加一个或多个 ChannelHandler。这些 ChannelHandler 将按照添加的顺序被调用。
     *
     * @param handlers
     * @return
     */
    ChannelPipeline addLast(ChannelHandler... handlers);

    /**
     * 向管道的末尾添加一个或多个 ChannelHandler。这些 ChannelHandler 将按照添加的顺序被调用。
     *
     * @param group
     * @param handlers
     * @return
     */
    ChannelPipeline addLast(EventExecutorGroup group, ChannelHandler... handlers);

    /**
     * ，用于在指定的 ChannelHandler 之前添加一个新的 ChannelHandler。
     *
     * @param baseName
     * @param name
     * @param handler
     * @return
     */
    ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler);


    /**
     * ，用于在指定的 ChannelHandler 之前添加一个新的 ChannelHandler。
     *
     * @param group
     * @param baseName
     * @param name
     * @param handler
     * @return
     */
    ChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name, ChannelHandler handler);

    /**
     * 用于在指定的 ChannelHandler 之后添加一个新的 ChannelHandler。
     *
     * @param baseName
     * @param name
     * @param handler
     * @return
     */
    ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler);

    /**
     * 用于在指定的 ChannelHandler 之后添加一个新的 ChannelHandler。
     *
     * @param group
     * @param baseName
     * @param name
     * @param handler
     * @return
     */
    ChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name, ChannelHandler handler);


    /**
     * 从管道中移除指定的 ChannelHandler。
     *
     * @param handler
     * @return
     */
    ChannelPipeline remove(ChannelHandler handler);

    /**
     * 从管道中移除指定的 ChannelHandler。
     *
     * @param name
     * @return
     */
    ChannelHandler remove(String name);

    /**
     * 从管道中移除指定的 ChannelHandler。
     *
     * @param handlerType
     * @param <T>
     * @return
     */
    <T extends ChannelHandler> T remove(Class<T> handlerType);

    /**
     * 从管道中移除指定的 ChannelHandler。
     *
     * @return
     */
    ChannelHandler removeFirst();

    /**
     * 从管道中移除指定的 ChannelHandler。
     *
     * @return
     */
    ChannelHandler removeLast();

    /**
     * r): 替换管道中的指定 ChannelHandler。可以指定新的 ChannelHandler 的名称。
     *
     * @param oldHandler
     * @param newName
     * @param newHandler
     * @return
     */
    ChannelPipeline replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler);

    /**
     * r): 替换管道中的指定 ChannelHandler。可以指定新的 ChannelHandler 的名称。
     *
     * @param oldName
     * @param newName
     * @param newHandler
     * @return
     */
    ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler);

    /**
     * 替换管道中的指定 ChannelHandler。可以指定新的 ChannelHandler 的名称。
     *
     * @param oldHandlerType
     * @param newName
     * @param newHandler
     * @param <T>
     * @return
     */
    <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName, ChannelHandler newHandler);

    /**
     * 获取当前ChannelPipeline中第一个ChannelHandler的引用。
     *
     * @return
     */
    ChannelHandler first();

    ChannelHandlerContext firstContext();

    /**
     * 获取当前ChannelPipeline中最后一个ChannelHandler的引用。
     *
     * @return
     */
    ChannelHandler last();

    ChannelHandlerContext lastContext();

    /**
     * 根据名字获取指定的handler
     *
     * @param name
     * @return
     */
    ChannelHandler get(String name);

    /**
     * 根据class 获取对于的 handler
     *
     * @param handlerType
     * @param <T>
     * @return
     */
    <T extends ChannelHandler> T get(Class<T> handlerType);

    /**
     * 获取当前ChannelHandler的上下文对象。
     *
     * @param handler
     * @return
     */
    ChannelHandlerContext context(ChannelHandler handler);

    /**
     * 获取当前ChannelHandler的上下文对象。
     *
     * @param name
     * @return
     */
    ChannelHandlerContext context(String name);

    /**
     * 获取当前ChannelHandler的上下文对象。
     *
     * @param handlerType
     * @return
     */
    ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType);

    /**
     * 对于的Channel，每个channel都有一个channelpeline
     *
     * @return
     */
    Channel channel();

    /**
     * 名字集合
     *
     * @return
     */
    List<String> names();

    /**
     * map 衍射
     *
     * @return
     */
    Map<String, ChannelHandler> toMap();

    @Override
    ChannelPipeline fireChannelRegistered();

    @Override
    ChannelPipeline fireChannelUnregistered();

    @Override
    ChannelPipeline fireChannelActive();

    @Override
    ChannelPipeline fireChannelInactive();

    @Override
    ChannelPipeline fireExceptionCaught(Throwable cause);

    @Override
    ChannelPipeline fireUserEventTriggered(Object event);

    @Override
    ChannelPipeline fireChannelRead(Object msg);

    @Override
    ChannelPipeline fireChannelReadComplete();

    @Override
    ChannelPipeline fireChannelWritabilityChanged();

    @Override
    ChannelPipeline flush();
}
