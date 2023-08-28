package com.ytrue.job.core.biz.client;

import com.ytrue.job.core.biz.AdminBiz;
import com.ytrue.job.core.biz.model.RegistryParam;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.util.XxlJobRemotingUtil;

/**
 * @author ytrue
 * @date 2023-08-28 11:12
 * @description AdminBizClient
 */
public class AdminBizClient implements AdminBiz {


    /**
     * 这里的地址是调度中心的服务地址
     */
    private String addressUrl;

    /**
     * token令牌，执行器和调度中心两端要一致
     */
    private String accessToken;

    /**
     * 访问超时时间
     */
    private int timeout = 3;

    public AdminBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
        if (!this.addressUrl.endsWith("/")) {
            this.addressUrl = this.addressUrl + "/";
        }
    }


    /**
     * 调用工具类发送post请求，访问调度中心，这个方法时将执行器注册到调度中心的方法
     *
     * @param registryParam
     * @return
     */
    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/registry", accessToken, timeout, registryParam, String.class);
    }

    /**
     * 这个方法是通知调度中心，把该执行器移除的方法
     *
     * @param registryParam
     * @return
     */
    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/registryRemove", accessToken, timeout, registryParam, String.class);
    }
}
