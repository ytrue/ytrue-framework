package com.ytrue.netty.util;

/**
 * @author ytrue
 * @date 2023-08-10 9:08
 * @description 资源泄漏跟踪器
 */
public interface ResourceLeakTracker<T>  {

    void record();


    void record(Object hint);


    boolean close(T trackedObject);
}
