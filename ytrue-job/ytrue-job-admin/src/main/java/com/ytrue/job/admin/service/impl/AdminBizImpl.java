package com.ytrue.job.admin.service.impl;

import com.ytrue.job.admin.core.thread.JobCompleteHelper;
import com.ytrue.job.admin.core.thread.JobRegistryHelper;
import com.ytrue.job.core.biz.AdminBiz;
import com.ytrue.job.core.biz.model.HandleCallbackParam;
import com.ytrue.job.core.biz.model.RegistryParam;
import com.ytrue.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-30 9:43
 * @description AdminBizImpl
 */
@Service
public class AdminBizImpl implements AdminBiz {
    /**
     * 把执行器注册到注册中心
     *
     * @param registryParam
     * @return
     */
    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        //通过JobRegistryHelper组件中创建的线程池来完成注册任务
        return JobRegistryHelper.getInstance().registry(registryParam);
    }

    /**
     * 移除执行器的方法
     *
     * @param registryParam
     * @return
     */
    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registryRemove(registryParam);
    }

    /**
     * 调度中心要调用的方法，把执行器回调的定时任务执行的结果信息收集起来
     *
     * @param callbackParamList
     * @return
     */
    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return JobCompleteHelper.getInstance().callback(callbackParamList);
    }
}
