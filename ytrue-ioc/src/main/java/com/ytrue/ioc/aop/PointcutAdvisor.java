package com.ytrue.ioc.aop;

/**
 * @author ytrue
 * @date 2022/10/14 09:56
 * @description PointcutAdvisor 承担了 Pointcut 和 Advice 的组合，Pointcut 用于获取 JoinPoint，而 Advice 决定于 JoinPoint 执行什么操作。
 */
public interface PointcutAdvisor extends Advisor {

    /**
     * 获取切点
     * @return
     */
    Pointcut getPointcut();
}
