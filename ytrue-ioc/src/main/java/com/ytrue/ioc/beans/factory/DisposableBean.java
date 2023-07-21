package com.ytrue.ioc.beans.factory;

/**
 * @author ytrue
 * @date 2022/10/11 15:33
 * @description DisposableBean
 */
public interface DisposableBean {

    /**
     * 销毁时触发
     *
     * @throws Exception
     */
    void destroy() throws Exception;
}
