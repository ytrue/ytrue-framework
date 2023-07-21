package com.ytrue.orm.cache;

/**
 * @author ytrue
 * @date 2022/9/7 15:39
 * @description SPI(Service Provider Interface) for cache providers. 缓存接口
 */
public interface Cache {

    /**
     * 获取ID，每个缓存都有唯一ID标识
     *
     * @return
     */
    String getId();

    /**
     * 存入值
     *
     * @param key
     * @param value
     */
    void putObject(Object key, Object value);

    /**
     * 获取值
     *
     * @param key
     * @return
     */
    Object getObject(Object key);

    /**
     * 删除值
     *
     * @param key
     * @return
     */
    Object removeObject(Object key);

    /**
     * 清空
     */
    void clear();

    /**
     * 获取缓存大小
     *
     * @return
     */
    int getSize();

}
