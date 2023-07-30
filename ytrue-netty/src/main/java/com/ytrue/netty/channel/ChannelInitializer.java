package com.ytrue.netty.channel;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ytrue
 * @date 2023/7/30 15:02
 * @description 至关重要的一个类，不管是客户端还是服务端，我们添加多个ChannelHandler时用的都是这个类
 * 首先这个类本身就是一个handler，这就意味着，一旦channel注册成功，该handler的handlerAdded方法就会被回调
 * 那我们看看handlerAdded方法究竟做了什么
 */
@ChannelHandler.Sharable
@Slf4j
public abstract class ChannelInitializer<C extends Channel> extends ChannelInboundHandlerAdapter {


    /**
     * 这个set集合是为了防止已经添加了该handler的ChannelPipeline再次被初始化，主要用于
     * 服务端接受到的客户端的连接。在服务端会有很多客户端的连接，也就有很多ChannelPipeline，而ChannelInitializer类型的
     * handler是被所有客户端channel共享的，所以每初始化一次ChannelPipeline，向其中添加handler时，就要判断该类的handler有没有添加过
     * 添加过就不再添加了，主要逻辑都在handlerAdded方法中，因为handlerAdded方法是最先被回调的方法
     */
    private final Set<ChannelHandlerContext> initMap = Collections.newSetFromMap(new ConcurrentHashMap<ChannelHandlerContext, Boolean>());


    /**
     * 初始化
     * @param ch
     * @throws Exception
     */
    protected abstract void initChannel(C ch) throws Exception;

    @Override
    @SuppressWarnings("unchecked")
    public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (initChannel(ctx)) {
            ctx.pipeline().fireChannelRegistered();
            removeState(ctx);
        } else {
            ctx.fireChannelRegistered();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (log.isWarnEnabled()) {
            log.warn("Failed to initialize a channel. Closing: " + ctx.channel(), cause);
        }
        ctx.close();
    }

    /**
     * 该方法会调用initChannel方法，而initChannel方法是个抽象方法，由用户自己在实现类中实现而实现通常是向ChannelPipeline中添加handler，在测试中我会给出具体例子
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isRegistered()) {
            if (initChannel(ctx)) {
                //初始化channel完毕后，需要从initMap删除该handler
                removeState(ctx);
            }
        }
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        initMap.remove(ctx);
    }

    @SuppressWarnings("unchecked")
    private boolean initChannel(ChannelHandlerContext ctx) throws Exception {
        if (initMap.add(ctx)) {
            try {
                //调用initChannel抽象方法，该方法由用户实现
                initChannel((C) ctx.channel());
            } catch (Throwable cause) {
                exceptionCaught(ctx, cause);
            } finally {
                ChannelPipeline pipeline = ctx.pipeline();
                //这里会发现只要该handler使用完毕，就会从ChannelPipeline中删除
                if (pipeline.context(this) != null) {
                    //删除时会调用该方法，在该方法内，会对该handler的handlerRemoved方法进行回调
                    pipeline.remove(this);
                }
            }
            return true;
        }
        return false;
    }



    private void removeState(final ChannelHandlerContext ctx) {
        if (ctx.isRemoved()) {
            initMap.remove(ctx);
        } else {
            ctx.executor().execute(() -> initMap.remove(ctx));
        }
    }
}
