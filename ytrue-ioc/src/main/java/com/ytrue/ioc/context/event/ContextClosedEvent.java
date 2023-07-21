package com.ytrue.ioc.context.event;

/**
 * @author ytrue
 * @date 2022/10/12 14:25
 * @description Spring 框架自己实现的两个事件类，可以用于关闭动作
 */
public class ContextClosedEvent extends  ApplicationContextEvent {

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ContextClosedEvent(Object source) {
        super(source);
    }
}
