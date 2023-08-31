package com.ytrue.job.admin.core.route.strategy;

import com.ytrue.job.admin.core.route.ExecutorRouter;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.biz.model.TriggerParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author ytrue
 * @date 2023-08-31 9:34
 * @description 最近最久未使用路由策略
 */
public class ExecutorRouteLRU extends ExecutorRouter {


    /**
     * 所谓按顺序访问就是在get的时候把访问的数据放到Map内部双向链表的尾部了，这样就会让没怎么被使用的数据来到双向链表
     * 头部，从头节点取出的第一个数据就是最近最久未使用的数据
     * 这属于很基础的数据结构知识了，我就不再细说了
     */
    private static final ConcurrentMap<Integer, LinkedHashMap<String, String>> jobLRUMap = new ConcurrentHashMap<>();

    /**
     * Map中数据的缓存时间
     */
    private static long CACHE_VALID_TIME = 0;


    public String route(int jobId, List<String> addressList) {
        //判断当前时间是否大于Map的缓存时间
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            //如果大于，则意味着数据过期了，清除即可
            jobLRUMap.clear();
            //重新设置数据缓存有效期
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }

        //根据定时任务id从jobLRUMap中获得对应的Map
        LinkedHashMap<String, String> lruItem = jobLRUMap.get(jobId);
        if (lruItem == null) {
            //accessOrder为true就是让LinkedHashMap按访问顺序迭代的意思
            //默认是使用插入顺序迭代
            //如果为null说明该定时任务是第一次执行，所以要初始化一个Map
            lruItem = new LinkedHashMap<String, String>(16, 0.75f, true);
            //把Map放到jobLRUMap中
            jobLRUMap.putIfAbsent(jobId, lruItem);
        }

        //判断有没有新添加的执行器
        for (String address : addressList) {
            //如果有就把它加入到lruItem中
            if (!lruItem.containsKey(address)) {
                lruItem.put(address, address);
            }
        }


        //判断有没有过期的执行器
        List<String> delKeys = new ArrayList<>();
        for (String existKey : lruItem.keySet()) {
            if (!addressList.contains(existKey)) {
                delKeys.add(existKey);
            }
        }

        //有就把执行器删除
        if (delKeys.size() > 0) {
            for (String delKey : delKeys) {
                lruItem.remove(delKey);
            }
        }

        //使用迭代器得到第一个数据
        String eldestKey = lruItem.entrySet().iterator().next().getKey();
        //得到对应的执行器地址
        //返回执行器地址
        return lruItem.get(eldestKey);
    }

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = route(triggerParam.getJobId(), addressList);
        return new ReturnT<>(address);
    }
}
