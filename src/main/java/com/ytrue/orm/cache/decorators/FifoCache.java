package com.ytrue.orm.cache.decorators;

import com.ytrue.orm.cache.Cache;
import lombok.Getter;
import lombok.Setter;

import java.util.Deque;
import java.util.LinkedList;

/**
 * @author ytrue
 * @date 2022/9/9 15:34
 * @description FIFO (first in, first out) cache decorator  装饰器设计模式,就是会把之前的丢弃
 */
public class FifoCache implements Cache {

    /**
     * 缓存
     */
    private final Cache delegate;

    /**
     * 队列
     */
    private Deque<Object> keyList;

    /**
     * 队列的大小
     */
    @Setter
    private int size;

    public FifoCache(Cache delegate) {
        this.delegate = delegate;
        this.keyList = new LinkedList<>();
        this.size = 1024;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public void putObject(Object key, Object value) {
        cycleKeyList(key);
        delegate.putObject(key, value);
    }

    @Override
    public Object getObject(Object key) {
        return delegate.getObject(key);
    }

    @Override
    public Object removeObject(Object key) {
        return delegate.removeObject(key);
    }

    @Override
    public void clear() {
        delegate.clear();
        keyList.clear();
    }

    @Override
    public int getSize() {
        return delegate.getSize();
    }


    private void cycleKeyList(Object key) {
        keyList.addLast(key);
        if (keyList.size() > size) {
            Object oldestKey = keyList.removeFirst();
            delegate.removeObject(oldestKey);
        }
    }
}
