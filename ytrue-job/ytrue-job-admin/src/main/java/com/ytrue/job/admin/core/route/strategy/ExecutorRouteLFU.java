package com.ytrue.job.admin.core.route.strategy;

import com.ytrue.job.admin.core.route.ExecutorRouter;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.biz.model.TriggerParam;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author ytrue
 * @date 2023-08-31 9:34
 * @description 最不经常使用的路由策略，频率/次数
 */
public class ExecutorRouteLFU extends ExecutorRouter {


    /**
     * 这个Map缓存的key-value中的key就是定时任务的id，value是一个map，这个map中缓存的是执行器的地址和该地址被使用的次数
     */
    private static final ConcurrentMap<Integer, HashMap<String, Integer>> jobLfuMap = new ConcurrentHashMap<>();

    /**
     * Map中数据的缓存时间
     */
    private static long CACHE_VALID_TIME = 0;

    public String route(int jobId, List<String> addressList) {
        //判断当前时间是否大于Map的缓存时间
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            //如果大于，则意味着数据过期了，清除即可
            jobLfuMap.clear();
            //重新设置数据缓存有效期
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }

        //先通过定时任务的id从jobLfuMap中获得对应的value
        HashMap<String, Integer> lfuItemMap = jobLfuMap.get(jobId);
        if (lfuItemMap == null) {
            //如果value为空，则创建一个Map
            lfuItemMap = new HashMap<>();
            //把Map添加到jobLfuMap中
            jobLfuMap.putIfAbsent(jobId, lfuItemMap);   // 避免重复覆盖
        }

        //下面开始遍历执行器地址集合
        for (String address : addressList) {
            //首先是判断该执行器地址是不是第一次使用，如果是第一次使用，lfuItemMap里面肯定还没有数据
            //所以就要为第一次使用的执行器初始化一个对应的使用次数
            if (!lfuItemMap.containsKey(address) || lfuItemMap.get(address) > 1000000) {
                //初始化操作在这里，因为这个路由策略说到底也是根据最不常用的次数来选择执行器的，所以，这里即便给执行器初始化了一个使用次数，最后还要判断一下哪个最不常用
                //才选择哪个。这里做了一个随机数选择，从0到执行器集合的长度之间随机选一个数作为执行器对应的使用次数
                //当然，当执行器使用次数大于1000000也会触发这个初始化操作
                //这里之所以随机选择一次，是为了减轻执行器的压力，具体解释可以参考ExecutorRouteRound类中38-46行之间的解释
                //因为我添加注释的时候，先添加的那个类，所以详细注释也先写在那里面了，这里就不重复了
                lfuItemMap.put(address, new Random().nextInt(addressList.size()));
            }
        }

        //判断有没有过期的执行器
        List<String> delKeys = new ArrayList<>();
        for (String existKey : lfuItemMap.keySet()) {
            if (!addressList.contains(existKey)) {
                delKeys.add(existKey);
            }
        }

        //如果有就把过期的执行器从lfuItemMap中移除
        if (delKeys.size() > 0) {
            for (String delKey : delKeys) {
                lfuItemMap.remove(delKey);
            }
        }

        //下面就开始选择具体的执行器来执行定时任务了，把lfuItemMap中的数据转移到lfuItemList中
        List<Map.Entry<String, Integer>> lfuItemList = new ArrayList<>(lfuItemMap.entrySet());
        //将lfuItemList中的数据按照执行器的使用次数做排序
        Collections.sort(lfuItemList, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        //获取到的第一个就是使用次数最少的执行器
        Map.Entry<String, Integer> addressItem = lfuItemList.get(0);
        String minAddress = addressItem.getKey();
        //因为要是用它了，所以把执行器的使用次数加1
        addressItem.setValue(addressItem.getValue() + 1);
        //返回执行器地址
        return addressItem.getKey();
    }


    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = route(triggerParam.getJobId(), addressList);
        return new ReturnT<>(address);
    }
}
