package com.ytrue.gateway.core.session.defaults;

import com.ytrue.gateway.core.session.Configuration;
import com.ytrue.gateway.core.session.IGenericReferenceSessionFactory;
import com.ytrue.gateway.core.session.SessionServer;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author ytrue
 * @date 2023-09-06 14:23
 * @description GenericReferenceProxyFactory
 */
public class GenericReferenceSessionFactory implements IGenericReferenceSessionFactory {


    private final Logger logger = LoggerFactory.getLogger(IGenericReferenceSessionFactory.class);

    private final Configuration configuration;


    private static final ThreadPoolExecutor SESSION_SERVER_THREAD_POOL = new ThreadPoolExecutor(
            2,
            2,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> new Thread(r, "ytrue-gateway-core, GenericReferenceSessionFactory SESSION_SERVER_THREAD_POOL-" + r.hashCode()),
            (r, executor) -> {
                throw new RuntimeException("ytrue-gateway-core, GenericReferenceSessionFactory SESSION_SERVER_THREAD_POOL is EXHAUSTED!");
            }
    );

    public GenericReferenceSessionFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Future<Channel> openSession() throws ExecutionException, InterruptedException {
        // session服务器
        SessionServer server = new SessionServer(configuration);
        // 交给线程池处理
        Future<Channel> future = SESSION_SERVER_THREAD_POOL.submit(server);
        Channel channel = future.get();
        // 校验
        if (null == channel) {
            throw new RuntimeException("netty server start error channel is null");
        }

        // 直到激活
        while (!channel.isActive()) {
            logger.info("netty server gateway start Ing ...");
            TimeUnit.MILLISECONDS.sleep(500);
        }

        logger.info("netty server gateway start Done! {}", channel.localAddress());

        return future;
    }
}
