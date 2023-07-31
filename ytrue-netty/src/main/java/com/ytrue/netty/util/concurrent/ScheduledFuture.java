package com.ytrue.netty.util.concurrent;

/**
 * @author ytrue
 * @date 2023-07-31 9:53
 * @description ScheduledFuture
 */
@SuppressWarnings("ClassNameSameAsAncestorName")
public interface ScheduledFuture<V> extends Future<V>, java.util.concurrent.ScheduledFuture<V> {
}
