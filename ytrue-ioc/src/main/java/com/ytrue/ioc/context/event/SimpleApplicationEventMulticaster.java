package com.ytrue.ioc.context.event;

import com.ytrue.ioc.beans.factory.BeanFactory;
import com.ytrue.ioc.context.ApplicationEvent;
import com.ytrue.ioc.context.ApplicationListener;

/**
 * @author ytrue
 * @date 2022/10/12 14:36
 * @description SimpleApplicationEventMulticaster
 */
public class SimpleApplicationEventMulticaster extends AbstractApplicationEventMulticaster {

    public SimpleApplicationEventMulticaster(BeanFactory beanFactory) {
        setBeanFactory(beanFactory);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void multicastEvent(final ApplicationEvent event) {
        for (final ApplicationListener listener : getApplicationListeners(event)) {
            listener.onApplicationEvent(event);
        }
    }


}
