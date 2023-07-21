package com.ytrue.ioc.context;

import java.util.EventListener;

/**
 * @author ytrue
 * @date 2022/10/12 14:30
 * @description ApplicationListener
 */
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

    /**
     * Handle an application event.
     * @param event the event to respond to
     */
    void onApplicationEvent(E event);

}
