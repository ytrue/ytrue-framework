package com.ytrue.orm.cache;

import com.ytrue.orm.cache.decorators.TransactionalCache;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2022/9/9 15:54
 * @description 事务缓存，管理器
 */
public class TransactionalCacheManager {

    private Map<Cache, TransactionalCache> transactionalCaches = new HashMap<>();


    /**
     * 清除某个TransactionalCache里面的缓存
     * @param cache
     */
    public void clear(Cache cache) {
        getTransactionalCache(cache).clear();
    }

    /**
     * 得到某个TransactionalCache的值
     */
    public Object getObject(Cache cache, CacheKey key) {
        return getTransactionalCache(cache).getObject(key);
    }

    public void putObject(Cache cache, CacheKey key, Object value) {
        getTransactionalCache(cache).putObject(key, value);
    }

    /**
     * 提交时全部提交
     */
    public void commit() {
        for (TransactionalCache txCache : transactionalCaches.values()) {
            txCache.commit();
        }
    }


    /**
     * 回滚时全部回滚
     */
    public void rollback() {
        for (TransactionalCache txCache : transactionalCaches.values()) {
            txCache.rollback();
        }
    }


    /**
     * 根据cache 获取对应的 TransactionalCache 没有则创建一个
     * @param cache
     * @return
     */
    private TransactionalCache getTransactionalCache(Cache cache) {
        TransactionalCache txCache = transactionalCaches.get(cache);
        if (txCache == null) {
            txCache = new TransactionalCache(cache);
            transactionalCaches.put(cache, txCache);
        }
        return txCache;
    }
}
