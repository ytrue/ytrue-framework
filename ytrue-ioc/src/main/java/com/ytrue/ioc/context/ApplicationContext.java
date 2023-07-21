package com.ytrue.ioc.context;

import com.ytrue.ioc.beans.factory.HierarchicalBeanFactory;
import com.ytrue.ioc.beans.factory.ListableBeanFactory;
import com.ytrue.ioc.core.io.ResourceLoader;

/**
 * @author ytrue
 * @date 2022/10/10 15:51
 * @description ApplicationContext
 */
public interface ApplicationContext extends ListableBeanFactory, HierarchicalBeanFactory, ResourceLoader, ApplicationEventPublisher {
}
