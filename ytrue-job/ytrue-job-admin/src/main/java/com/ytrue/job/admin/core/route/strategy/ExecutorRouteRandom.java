package com.ytrue.job.admin.core.route.strategy;

import com.ytrue.job.admin.core.route.ExecutorRouter;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.biz.model.TriggerParam;

import java.util.List;
import java.util.Random;

/**
 * @author ytrue
 * @date 2023-08-31 9:36
 * @description 随机选择一个执行器地址
 */
public class ExecutorRouteRandom extends ExecutorRouter {

    private static final Random localRandom = new Random();

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = addressList.get(localRandom.nextInt(addressList.size()));
        return new ReturnT<>(address);
    }

}
