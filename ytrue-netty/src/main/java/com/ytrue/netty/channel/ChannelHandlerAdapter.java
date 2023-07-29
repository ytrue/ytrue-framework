package com.ytrue.netty.channel;

/**
 * @author ytrue
 * @date 2023/7/29 10:50
 * @description ChannelHandlerAdapter
 */
public abstract class ChannelHandlerAdapter implements ChannelHandler {

    boolean added;

    protected void ensureNotSharable() {
        if (isSharable()) {
            throw new IllegalStateException("ChannelHandler " + getClass().getName() + " is not allowed to be shared");
        }
    }

    /**
     * 判断是不是可以共享，通过反射获取类似是否有这个注解 @Sharable
     *
     * @return
     */
    public boolean isSharable() {
//        Class<?> clazz = getClass();
//        Map<Class<?>, Boolean> cache = InternalThreadLocalMap.get().handlerSharableCache();
//        Boolean sharable = cache.get(clazz);
//        if (sharable == null) {
//            sharable = clazz.isAnnotationPresent(Sharable.class);
//            cache.put(clazz, sharable);
//        }
//        return sharable;
        return true;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // NOOP
    }


    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // NOOP
    }

    @ChannelHandlerMask.Skip
    @Override
    @Deprecated
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }
}
