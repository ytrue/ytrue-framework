package com.ytrue.orm.session;

/**
 * @author ytrue
 * @date 2022/9/7 15:36
 * @description 本地缓存机制；
 * SESSION 默认值，缓存一个会话中执行的所有查询
 * STATEMENT 本地会话仅用在语句执行上，对相同 SqlSession 的不同调用将不做数据共享
 */
public enum LocalCacheScope {
    /**
     * 类型
     */
    SESSION,
    STATEMENT
}
