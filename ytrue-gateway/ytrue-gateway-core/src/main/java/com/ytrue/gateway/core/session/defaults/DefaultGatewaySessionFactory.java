package com.ytrue.gateway.core.session.defaults;

import com.ytrue.gateway.core.session.Configuration;
import com.ytrue.gateway.core.session.GatewaySession;
import com.ytrue.gateway.core.session.GatewaySessionFactory;

/**
 * @author ytrue
 * @date 2023-09-06 17:02
 * @description DefaultGatewaySessionFactory
 */
public class DefaultGatewaySessionFactory implements GatewaySessionFactory {

    private final Configuration configuration;

    public DefaultGatewaySessionFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public GatewaySession openSession() {
        return new DefaultGatewaySession(configuration);
    }

}
