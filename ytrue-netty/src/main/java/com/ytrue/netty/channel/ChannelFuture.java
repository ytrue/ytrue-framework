package com.ytrue.netty.channel;

import com.ytrue.netty.util.concurrent.Future;
import com.ytrue.netty.util.concurrent.GenericFutureListener;

/**
 * @author ytrue
 * @date 2023-07-26 9:03
 * @description 和channel有关的future
 * ChannelFuture的主要作用如下：
 * 1. 异步操作的结果占位符：ChannelFuture表示一个异步操作的结果的占位符，可以通过调用其方法来获取操作的结果。
 * 2. 异步操作的监听器：可以通过ChannelFuture的addListener()方法添加监听器，以便在异步操作完成时得到通知。可以通过监听器来处理操作成功或失败的情况，并执行相应的逻辑。
 * 3. 控制操作的执行顺序：ChannelFuture提供了一些方法，如sync()、await()等，可以用于控制操作的执行顺序。比如，可以使用sync()方法来等待异步操作的完成，以确保在继续执行后续代码之前，先等待操作完成。
 * 4. 操作结果的判断和处理：ChannelFuture提供了一些方法，如isSuccess()、isDone()等，可以用于判断操作是否成功完成、是否已经完成等。根据操作的结果，可以进行相应的处理，如获取操作的结果、处理异常等。
 * 总之，ChannelFuture在Netty中扮演着重要的角色，用于表示异步操作的结果，并提供了一系列方法来获取操作结果、添加监听器以及进行操作的控制。通过ChannelFuture，可以实现异步操作的处理和控制，以及对操作结果的判断和处理。
 */
public interface ChannelFuture extends Future<Void> {

    Channel channel();

    @Override
    ChannelFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelFuture sync() throws InterruptedException;

    @Override
    ChannelFuture syncUninterruptibly();

    @Override
    ChannelFuture await() throws InterruptedException;

    @Override
    ChannelFuture awaitUninterruptibly();

    boolean isVoid();
}
