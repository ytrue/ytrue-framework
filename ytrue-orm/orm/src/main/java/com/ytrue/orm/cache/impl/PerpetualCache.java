package com.ytrue.orm.cache.impl;

import com.ytrue.orm.cache.Cache;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ytrue
 * @date 2022/9/7 15:41
 * @description 一级缓存，在 Session 生命周期内一直保持，每创建新的 OpenSession 都会创建一个缓存器 PerpetualCache
 */
@Slf4j
public class PerpetualCache implements Cache {

    private String id;

    /**
     * 使用HashMap存放一级缓存数据，session 生命周期较短，正常情况下数据不会一直在缓存存放
     */
    private Map<Object, Object> cache = new HashMap<>();

    public PerpetualCache(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void putObject(Object key, Object value) {
        cache.put(key, value);
    }

    @Override
    public Object getObject(Object key) {
        return cache.get(key);
    }

    @Override
    public Object removeObject(Object key) {
        return cache.remove(key);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public int getSize() {
        return cache.size();
    }
}
