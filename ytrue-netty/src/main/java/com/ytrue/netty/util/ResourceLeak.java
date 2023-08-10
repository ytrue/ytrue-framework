package com.ytrue.netty.util;

/**
 * @author ytrue
 * @date 2023-08-10 9:14
 * @description ResourceLeak
 */
@Deprecated
public interface ResourceLeak {

    void record();


    void record(Object hint);


    boolean close();
}
