package com.ytrue.ioc.aop.framework;

import com.ytrue.ioc.aop.AdvisedSupport;

/**
 * @author ytrue
 * @date 2022/10/14 10:04
 * @description ProxyFactory
 */
public class ProxyFactory {

    private AdvisedSupport advisedSupport;

    public ProxyFactory(AdvisedSupport advisedSupport) {
        this.advisedSupport = advisedSupport;
    }

    public Object getProxy() {
        return createAopProxy().getProxy();
    }

    private AopProxy createAopProxy() {
        if (advisedSupport.isProxyTargetClass()) {
            return new Cglib2AopProxy(advisedSupport);
        }

        return new JdkDynamicAopProxy(advisedSupport);
    }
}
