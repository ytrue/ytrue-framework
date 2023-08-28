package com.ytrue.job.core.biz.client;

import com.ytrue.job.core.biz.ExecutorBiz;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.biz.model.TriggerParam;
import com.ytrue.job.core.util.XxlJobRemotingUtil;

/**
 * @author ytrue
 * @date 2023-08-28 11:17
 * @description ExecutorBizClient
 */
public class ExecutorBizClient implements ExecutorBiz {

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

    public ExecutorBizClient(String addressUrl, String accessToken) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
        if (!this.addressUrl.endsWith("/")) {
            this.addressUrl = this.addressUrl + "/";
        }
    }


    /**
     * 远程调用的方法
     *
     * @param triggerParam
     * @return
     */
    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        //可以看到，在这里直接用一个工具类用post请求发送消息了
        return XxlJobRemotingUtil.postBody(addressUrl + "run", accessToken, timeout, triggerParam, String.class);
    }

}
