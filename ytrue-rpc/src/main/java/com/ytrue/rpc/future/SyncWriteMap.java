package com.ytrue.rpc.future;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ytrue
 * @date 2023-05-20 13:53
 * @description 缓存
 */
public class SyncWriteMap {
    public static Map<String, WriteFuture> syncKey = new ConcurrentHashMap<>();
}
