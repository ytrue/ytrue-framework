package com.ytrue.orm.session;

/**
 * @author ytrue
 * @date 2022/7/11 11:18
 * @description SqlSessionFactory
 */
public interface SqlSessionFactory {

    /**
     * 打开一个 session
     * @return SqlSession
     */
    SqlSession openSession();
}
