package com.ytrue.netty.bootstrap;

import com.ytrue.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

/**
 * @author ytrue
 * @date 2023-07-28 15:34
 * @description BootstrapConfig
 */
@Slf4j
public class BootstrapConfig extends AbstractBootstrapConfig<Bootstrap, Channel> {

    BootstrapConfig(Bootstrap bootstrap) {
        super(bootstrap);
    }


    public SocketAddress remoteAddress() {
        return bootstrap.remoteAddress();
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.setLength(buf.length() - 1);
        buf.append(", resolver: ");
        SocketAddress remoteAddress = remoteAddress();
        if (remoteAddress != null) {
            buf.append(", remoteAddress: ")
                    .append(remoteAddress);
        }
        return buf.append(')').toString();
    }
}
