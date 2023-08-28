package com.ytrue.job.core.biz;

import com.ytrue.job.core.biz.model.RegistryParam;
import com.ytrue.job.core.biz.model.ReturnT;

/**
 * @author ytrue
 * @date 2023-08-28 11:02
 * @description 程序内部使用的接口，该接口是调度中心暴露给执行器那一端的
 */
public interface AdminBiz {

    /**
     * 执行器注册自己到调度中心的方法
     *
     * @param registryParam
     * @return
     */
    ReturnT<String> registry(RegistryParam registryParam);

    /**
     * 执行器将自己从调度中心移除的方法
     *
     * @param registryParam
     * @return
     */
    ReturnT<String> registryRemove(RegistryParam registryParam);
}
