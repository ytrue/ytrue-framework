package com.ytrue.netty.handler.timeout;

import com.ytrue.netty.channel.ChannelHandlerContext;

import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-08-01 10:10
 * @description ReadTimeoutHandler
 */
public class ReadTimeoutHandler extends IdleStateHandler {
    private boolean closed;


    public ReadTimeoutHandler(int timeoutSeconds) {
        this(timeoutSeconds, TimeUnit.SECONDS);
    }


    public ReadTimeoutHandler(long timeout, TimeUnit unit) {
        super(timeout, 0, 0, unit);
    }

    @Override
    protected final void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        assert evt.state() == IdleState.READER_IDLE;
        readTimedOut(ctx);
    }


    protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
        if (!closed) {
            ctx.fireExceptionCaught(ReadTimeoutException.INSTANCE);
            ctx.close();
            closed = true;
        }
    }
}
