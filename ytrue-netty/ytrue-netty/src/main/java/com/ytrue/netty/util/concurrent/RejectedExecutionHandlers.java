package com.ytrue.netty.util.concurrent;

import java.util.concurrent.RejectedExecutionException;

/**
 * @author ytrue
 * @date 2023-07-22 13:52
 * @description RejectedExecutionHandlers
 */
public class RejectedExecutionHandlers {

    private static final RejectedExecutionHandler REJECT = (task, executor) -> {
        throw new RejectedExecutionException();
    };

    private RejectedExecutionHandlers() {
    }


    public static RejectedExecutionHandler reject() {
        return REJECT;
    }
}
