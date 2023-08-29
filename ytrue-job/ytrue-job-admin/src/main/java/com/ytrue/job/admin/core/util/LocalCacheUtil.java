package com.ytrue.job.admin.core.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author ytrue
 * @date 2023-08-29 9:39
 * @description 本地缓存的工具类
 */
public class LocalCacheUtil {

    /**
     * 这个Map就是缓存数据的容器
     */
    private static ConcurrentMap<String, LocalCacheData> cacheRepository = new ConcurrentHashMap<>();


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class LocalCacheData {
        /**
         * key
         */
        private String key;

        /**
         * 值
         */
        private Object val;
        /**
         * 超时时间
         */
        private long timeoutTime;
    }


    /**
     * 存储键值对的方法
     *
     * @param key
     * @param val
     * @param cacheTime
     * @return
     */
    public static boolean set(String key, Object val, long cacheTime) {
        //先清除一次缓存超时的数据
        cleanTimeoutCache();
        //对key-value-缓存时间做判空
        if (key == null || key.trim().length() == 0) {
            return false;
        }
        if (val == null) {
            remove(key);
        }
        if (cacheTime <= 0) {
            remove(key);
        }
        //获取当前缓存数据的超时时间
        long timeoutTime = System.currentTimeMillis() + cacheTime;
        //创建缓存键值对的对象
        LocalCacheData localCacheData = new LocalCacheData(key, val, timeoutTime);
        //放入Map中
        cacheRepository.put(localCacheData.getKey(), localCacheData);
        return true;
    }


    /**
     * 删除键值对
     *
     * @param key
     * @return
     */
    public static boolean remove(String key) {
        if (key == null || key.trim().length() == 0) {
            return false;
        }
        cacheRepository.remove(key);
        return true;
    }

    /**
     * 获取数据
     *
     * @param key
     * @return
     */
    public static Object get(String key) {
        if (key == null || key.trim().length() == 0) {
            return null;
        }
        LocalCacheData localCacheData = cacheRepository.get(key);
        // 判断是否有数据，并且数据没有过期
        if (localCacheData != null && System.currentTimeMillis() < localCacheData.getTimeoutTime()) {
            return localCacheData.getVal();
        } else {
            remove(key);
            return null;
        }
    }


    /**
     * 清楚超时的缓存键值对
     *
     * @return
     */
    public static boolean cleanTimeoutCache() {
        //先判断Map中是否有数据
        if (!cacheRepository.keySet().isEmpty()) {
            //如果有数据就遍历键值对
            for (String key : cacheRepository.keySet()) {
                //获取每一个LocalCacheData对象
                LocalCacheData localCacheData = cacheRepository.get(key);
                //判断LocalCacheData对象中的超时时间是否到了
                if (localCacheData != null && System.currentTimeMillis() >= localCacheData.getTimeoutTime()) {
                    //如果到达超时时间就删除键值对
                    cacheRepository.remove(key);
                }
            }
        }
        return true;
    }

}
