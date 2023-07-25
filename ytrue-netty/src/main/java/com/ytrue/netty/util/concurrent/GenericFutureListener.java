package com.ytrue.netty.util.concurrent;

import java.util.EventListener;

/**
 * @author ytrue
 * @date 2023-07-25 9:09
 * @description 通用的FutureListener
 */
public interface  GenericFutureListener<F extends Future<?>> extends EventListener {

    /**
     * 完成后的回调
     * @param future
     * @throws Exception
     */
    void operationComplete(F future) throws Exception;
}
