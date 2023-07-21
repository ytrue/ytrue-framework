package com.ytrue.ioc.context.event;

import com.ytrue.ioc.context.ApplicationContext;
import com.ytrue.ioc.context.ApplicationEvent;

/**
 * @author ytrue
 * @date 2022/10/12 14:23
 * @description ApplicationContextEvent 是定义事件的抽象类，所有的事件包括关闭、刷新，以及用户自己实现的事件，都需要继承这个类。
 */
public class ApplicationContextEvent extends ApplicationEvent {

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ApplicationContextEvent(Object source) {
        super(source);
    }

    /**
     * Get the <code>ApplicationContext</code> that the event was raised for.
     *
     * @return
     */
    public final ApplicationContext getApplicationContext() {
        return (ApplicationContext) getSource();
    }

}
