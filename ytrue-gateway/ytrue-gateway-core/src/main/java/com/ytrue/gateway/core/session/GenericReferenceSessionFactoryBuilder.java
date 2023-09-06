package com.ytrue.gateway.core.session;

import com.ytrue.gateway.core.session.defaults.GenericReferenceSessionFactory;
import io.netty.channel.Channel;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author ytrue
 * @date 2023-09-06 14:23
 * @description GenericReferenceSessionFactoryBuilder，参考mybatis的sessionFactory创建
 */
public class GenericReferenceSessionFactoryBuilder {


    /**
     * 构建IGenericReferenceSessionFactory
     *
     * @param configuration
     * @return
     */
    public Future<Channel> build(Configuration configuration) {
        IGenericReferenceSessionFactory genericReferenceSessionFactory = new GenericReferenceSessionFactory(configuration);
        try {
            return genericReferenceSessionFactory.openSession();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
