package com.ytrue.gateway.core.session;

import io.netty.channel.Channel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author ytrue
 * @date 2023-09-06 14:23
 * @description IGenericReferenceSessionFactory
 */
public interface IGenericReferenceSessionFactory {

    /**
     * 创建session
     *
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    Future<Channel> openSession() throws ExecutionException, InterruptedException;
}
