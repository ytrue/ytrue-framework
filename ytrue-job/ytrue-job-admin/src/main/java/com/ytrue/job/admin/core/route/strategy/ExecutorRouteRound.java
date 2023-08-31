package com.ytrue.job.admin.core.route.strategy;

import com.ytrue.job.admin.core.route.ExecutorRouter;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.biz.model.TriggerParam;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ytrue
 * @date 2023-08-31 9:34
 * @description 通过轮训的策略选择执行器地址
 */
public class ExecutorRouteRound extends ExecutorRouter {
    /**
     * 该Map的key为定时任务的id，value为一个次数，用于和地址集合的长度取余
     */
    private static final ConcurrentMap<Integer, AtomicInteger> routeCountEachJob = new ConcurrentHashMap<>();
    /**
     * Map中数据的缓存时间
     */
    private static long CACHE_VALID_TIME = 0;

    private static int count(int jobId) {
        //判断当前时间是否大于Map的缓存时间
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            //如果大于，则意味着数据过期了，清除即可
            routeCountEachJob.clear();
            //重新设置数据缓存有效期
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }
        //根据定时任务id从Map中取出对应的次数
        AtomicInteger count = routeCountEachJob.get(jobId);
        //如果是第一次执行这个定时任务，那么Map中肯定没有缓存着对应的定时任务id，也没有value
        //这里对应的做法就是初始化一个value，从0-100之间选择一个随机数，赋值给value
        if (count == null || count.get() > 1000000) {
            //这里其实还有一个操作，就是如果value对应的值已经大于1000000，也执行下面的这个初始化
            //重新给count赋值
            //但是这里为什么会有这个操作呢？为什么当定时任务第一次执行的时候，要弄一个随机数来取余呢？
            //这是因为每一个定时任务第一次执行的时候，如果不弄一个随机数做取余运算，那这些定时任务选择的执行器一定都是
            //相同的，如果第一时间有很多定时任务第一次执行，就会在同一个执行器上执行，可能会给执行器造成很大的压力
            //明明执行器也搭建了集群，却没有集群的效果，所以这里弄一个随机数，把第一次执行的定时任务分散到各个执行器上来执行
            //缓解执行器的压力
            //这样后面再执行相同的任务，调度的执行器也都分散了，压力会小很多的
            count = new AtomicInteger(new Random().nextInt(100));
        } else {
            //如果count有值，说明不是第一次执行这个定时任务了
            //在这里加1
            count.addAndGet(1);
        }
        //把value更新到Map中
        routeCountEachJob.put(jobId, count);
        //得到count的值
        return count.get();
    }

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        //这里就是简单的取余操作，在routeCountEachJob中会记录着定时任务对应的一个数值，这个数值会和执行器集合
        //的长度做%运算，得到要使用的执行器地址。而且定时任务每调度一次，它在routeCountEachJob中对应的
        //value就会加1，然后再和集合长度取余，这样就达到了轮训地址的效果。
        String address = addressList.get(count(triggerParam.getJobId()) % addressList.size());
        return new ReturnT<String>(address);
    }
}
