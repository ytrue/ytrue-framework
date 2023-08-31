package com.ytrue.job.admin.core.route.strategy;

import com.ytrue.job.admin.core.route.ExecutorRouter;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-31 9:34
 * @description 使用集合中最后一个地址
 */
public class ExecutorRouteLast extends ExecutorRouter {
    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ReturnT<>(addressList.get(addressList.size() - 1));
    }
}
