package com.ytrue.ioc.context;

import java.util.EventObject;

/**
 * @author ytrue
 * @date 2022/10/12 14:24
 * @description ApplicationEvent
 */
public abstract class ApplicationEvent extends EventObject {

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ApplicationEvent(Object source) {
        super(source);
    }
}
