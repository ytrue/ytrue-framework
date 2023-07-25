package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-25 9:17
 * @description 在netty中，返回的future中，基本都是该接口的实现类
 */
public interface Promise<V> extends Future<V> {

    /**
     * 标记当前Future成功，设置结果，如果设置成功，则通知所有的监听器，如果Future已经成功或者失败，则抛出IllegalStateException
     *
     * @param result
     * @return
     */
    Promise<V> setSuccess(V result);

    /**
     * 标记当前Future成功，设置结果，如果设置成功，则通知所有的监听器并且返回true，否则返回false
     *
     * @param result
     * @return
     */
    boolean trySuccess(V result);

    /**
     * 标记当前Future失败，设置结果为异常实例，如果设置成功，则通知所有的监听器，如果Future已经成功或者失败，则抛出IllegalStateException
     *
     * @param cause
     * @return
     */
    Promise<V> setFailure(Throwable cause);


    /**
     * 标记当前Future失败，设置结果为异常实例，如果设置成功，则通知所有的监听器并且返回true，否则返回false
     *
     * @param cause
     * @return
     */
    boolean tryFailure(Throwable cause);

    /**
     * 标记当前的Promise实例为不可取消，设置成功返回true，否则返回false
     *
     * @return
     */
    boolean setUncancellable();

    // 下面的方法和io.netty.util.concurrent.Future中的方法基本一致，只是修改了返回类型为Promise
    @Override
    Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    Promise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    Promise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    Promise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    Promise<V> await() throws InterruptedException;

    @Override
    Promise<V> awaitUninterruptibly();

    @Override
    Promise<V> sync() throws InterruptedException;

    @Override
    Promise<V> syncUninterruptibly();
}
