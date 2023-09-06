package com.ytrue.gateway.core;

import com.ytrue.gateway.core.session.Configuration;
import com.ytrue.gateway.core.session.GenericReferenceSessionFactoryBuilder;
import io.netty.channel.Channel;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author ytrue
 * @date 2023-09-06 15:43
 * @description ApiTest
 */
public class ApiTest {

    private final Logger logger = LoggerFactory.getLogger(ApiTest.class);


    @Test
    public void test_GenericReference() throws InterruptedException, ExecutionException {
        Configuration configuration = new Configuration();
        configuration.addGenericReference(
                "api-gateway-test",
                "cn.bugstack.gateway.rpc.IActivityBooth",
                "sayHi");

        GenericReferenceSessionFactoryBuilder builder = new GenericReferenceSessionFactoryBuilder();
        Future<Channel> future = builder.build(configuration);

        logger.info("服务启动完成 {}", future.get().id());
        Thread.sleep(Long.MAX_VALUE);
    }
}
