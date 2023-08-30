package com.ytrue.job.admin.service.impl;

import com.ytrue.job.admin.core.thread.JobRegistryHelper;
import com.ytrue.job.core.biz.AdminBiz;
import com.ytrue.job.core.biz.model.RegistryParam;
import com.ytrue.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Service;

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
}
