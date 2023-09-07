package com.ytrue.gateway.core.executor;

import com.ytrue.gateway.core.datasource.Connection;
import com.ytrue.gateway.core.session.Configuration;

/**
 * @author ytrue
 * @date 2023-09-07 10:54
 * @description SimpleExecutor
 */
public class SimpleExecutor extends BaseExecutor {

    public SimpleExecutor(Configuration configuration, Connection connection) {
        super(configuration, connection);
    }


    @Override
    protected Object doExec(String methodName, String[] parameterTypes, Object[] args) {
        /*
         * 调用服务
         * 封装参数 PS：为什么这样构建参数，可以参考测试案例；cn.bugstack.gateway.test.RPCTest
         * 01(允许)：java.lang.String
         * 02(允许)：cn.bugstack.gateway.rpc.dto.XReq
         * 03(拒绝)：java.lang.String, cn.bugstack.gateway.rpc.dto.XReq —— 不提供多参数方法的处理
         * */
        return connection.execute(methodName, parameterTypes, new String[]{"ignore"}, args);
    }
}
