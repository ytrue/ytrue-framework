package com.ytrue.netty.bootstrap;

import com.ytrue.netty.channel.Channel;
import com.ytrue.netty.channel.EventLoopGroup;
import com.ytrue.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ytrue
 * @date 2023-07-28 15:30
 * @description ServerBootstrapConfig
 */
@Slf4j
public final class ServerBootstrapConfig extends AbstractBootstrapConfig<ServerBootstrap, Channel> {

    protected ServerBootstrapConfig(ServerBootstrap bootstrap) {
        super(bootstrap);
    }


    @SuppressWarnings("deprecation")
    public EventLoopGroup childGroup() {
        return bootstrap.childGroup();
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.setLength(buf.length() - 1);
        buf.append(", ");
        EventLoopGroup childGroup = childGroup();
        if (childGroup != null) {
            buf.append("childGroup: ");
            buf.append(StringUtil.simpleClassName(childGroup));
            buf.append(", ");
        }
        if (buf.charAt(buf.length() - 1) == '(') {
            buf.append(')');
        } else {
            buf.setCharAt(buf.length() - 2, ')');
            buf.setLength(buf.length() - 1);
        }

        return buf.toString();
    }
}
