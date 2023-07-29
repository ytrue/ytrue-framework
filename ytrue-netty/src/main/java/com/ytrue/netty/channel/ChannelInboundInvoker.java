package com.ytrue.netty.channel;

/**
 * @author ytrue
 * @date 2023/7/29 10:18
 * @description 这个接口定义了channel入站时候的方法
 */
public interface ChannelInboundInvoker {

    /**
     * 触发通道注册事件，通知通道已被注册到 EventLoop。
     *
     * @return
     */
    ChannelInboundInvoker fireChannelRegistered();

    /**
     * 触发通道取消注册事件，通知通道已被取消注册。
     *
     * @return
     */
    ChannelInboundInvoker fireChannelUnregistered();

    /**
     * 触发通道激活事件，通知通道已处于活动状态。
     *
     * @return
     */
    ChannelInboundInvoker fireChannelActive();

    /**
     * 触发通道非活动事件，通知通道已处于非活动状态。
     *
     * @return
     */
    ChannelInboundInvoker fireChannelInactive();

    /**
     * 触发异常捕获事件，通知发生了异常并传递异常原因。
     *
     * @param cause
     * @return
     */
    ChannelInboundInvoker fireExceptionCaught(Throwable cause);

    /**
     *触发用户自定义事件，通知发生了用户自定义的事件。
     *
     * @param event
     * @return
     */
    ChannelInboundInvoker fireUserEventTriggered(Object event);

    /**
     * 触发通道读取事件，通知有新的消息可供读取。
     *
     * @param msg
     * @return
     */
    ChannelInboundInvoker fireChannelRead(Object msg);

    /**
     * 触发通道读取完成事件，通知通道上的所有数据都已读取完毕。
     *
     * @return
     */
    ChannelInboundInvoker fireChannelReadComplete();

    /**
     * 触发通道可写性变化事件，通知通道的可写性发生了变化。
     *
     * @return
     */
    ChannelInboundInvoker fireChannelWritabilityChanged();
}
