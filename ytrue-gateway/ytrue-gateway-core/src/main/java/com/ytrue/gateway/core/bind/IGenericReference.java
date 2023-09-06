package com.ytrue.gateway.core.bind;

/**
 * @author ytrue
 * @date 2023-09-06 14:18
 * @description 统一泛化调用接口
 */
public interface IGenericReference {


    /**
     * 调用
     *
     * @param args
     * @return
     */
    String $invoke(String args);
}
