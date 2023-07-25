package com.ytrue.netty;

import com.ytrue.netty.channel.EventLoop;
import com.ytrue.netty.channel.nio.NioEventLoopGroup;
import com.ytrue.netty.util.concurrent.DefaultPromise;
import com.ytrue.netty.util.concurrent.Future;
import com.ytrue.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-07-25 10:48
 * @description DefaultPromiseTest
 */
public class DefaultPromiseTest {

    public static void main(String[] args) throws InterruptedException {

        NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(1);

        EventLoop eventLoop = nioEventLoopGroup.next().next();

        DefaultPromise<Object> promise = new DefaultPromise<>(eventLoop);


        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            promise.setSuccess("ok");

            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("child thread end");

        }).start();


        promise.addListener(future -> System.out.println(future.get()));

        Thread.currentThread().join();
    }
}
