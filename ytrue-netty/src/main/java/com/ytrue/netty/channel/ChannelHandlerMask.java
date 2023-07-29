package com.ytrue.netty.channel;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author ytrue
 * @date 2023/7/29 11:06
 * @description ChannelHandlerMask类是Netty框架中的一个工具类，
 * 用于管理ChannelHandler的屏蔽（mask）操作。
 * 它提供了一种机制来标记某些ChannelHandler，使其在事件传播过程中被跳过或屏蔽。
 */
@Slf4j
public class ChannelHandlerMask {

    /**
     * 异常捕获事件
     */
    static final int MASK_EXCEPTION_CAUGHT = 1;

    /**
     * 通道注册事件 2
     */
    static final int MASK_CHANNEL_REGISTERED = 1 << 1;

    /**
     * 通道注销事件 4
     */
    static final int MASK_CHANNEL_UNREGISTERED = 1 << 2;

    /**
     * 通道激活事件  8
     */
    static final int MASK_CHANNEL_ACTIVE = 1 << 3;

    /**
     * 通道非活动事件 16
     */
    static final int MASK_CHANNEL_INACTIVE = 1 << 4;

    /**
     * 通道读取事件 32
     */
    static final int MASK_CHANNEL_READ = 1 << 5;

    /**
     * 通道读取完成事件 64
     */
    static final int MASK_CHANNEL_READ_COMPLETE = 1 << 6;

    /**
     * 用户自定义事件触发事件 128
     */
    static final int MASK_USER_EVENT_TRIGGERED = 1 << 7;

    /**
     * 通道可写性变化事件 256
     */
    static final int MASK_CHANNEL_WRITABILITY_CHANGED = 1 << 8;

    /**
     * 绑定事件 512----下面就是出站的事件了
     */
    static final int MASK_BIND = 1 << 9;

    /**
     * 连接事件 1024
     */
    static final int MASK_CONNECT = 1 << 10;

    /**
     * 断开连接事件 2048
     */
    static final int MASK_DISCONNECT = 1 << 11;

    /**
     * 关闭事件 4096
     */
    static final int MASK_CLOSE = 1 << 12;

    /**
     * 注销事件 8192
     */
    static final int MASK_DEREGISTER = 1 << 13;

    /**
     * 读取事件 16,384
     */
    static final int MASK_READ = 1 << 14;

    /**
     * 写事件 32,768
     */
    static final int MASK_WRITE = 1 << 15;

    /**
     * 刷新事件 65,536
     */
    static final int MASK_FLUSH = 1 << 16;


    /**
     * 入站处理器所拥有的所有常量事件，或运算做加法，相当于把所有事件加到一起
     */
    private static final int MASK_ALL_INBOUND = MASK_EXCEPTION_CAUGHT | MASK_CHANNEL_REGISTERED |
            MASK_CHANNEL_UNREGISTERED | MASK_CHANNEL_ACTIVE | MASK_CHANNEL_INACTIVE | MASK_CHANNEL_READ |
            MASK_CHANNEL_READ_COMPLETE | MASK_USER_EVENT_TRIGGERED | MASK_CHANNEL_WRITABILITY_CHANGED;

    /**
     * 出站处理器所拥有的所有常量事件
     */
    private static final int MASK_ALL_OUTBOUND = MASK_EXCEPTION_CAUGHT | MASK_BIND | MASK_CONNECT | MASK_DISCONNECT |
            MASK_CLOSE | MASK_DEREGISTER | MASK_READ | MASK_WRITE | MASK_FLUSH;


    /**
     * WeakHashMap是Java中的一种特殊的HashMap实现，它的作用是提供一种基于弱引用的键对象的映射关系。与普通的HashMap不同，WeakHashMap中的键对象是弱引用，
     * 这意味着如果键对象没有被其他强引用所引用，那么它会被垃圾回收器回收，从而使得该键值对从WeakHashMap中自动移除。
     * WeakHashMap的主要应用场景是在需要缓存对象时，可以使用弱引用来避免内存泄漏。当键对象不再被其他地方引用时，WeakHashMap会自动将其从缓存中移除，释放内存。
     * 另外，WeakHashMap还可以用于实现一些特殊的缓存策略，例如缓存一些临时数据，当内存紧张时，这些临时数据会被自动清理。
     * 需要注意的是，由于键对象是弱引用，所以在使用WeakHashMap时需要确保键对象没有其他强引用指向它，否则键值对可能不会被及时移除，从而导致内存泄漏的问题
     * <p>
     * WeakHashMap，当键对象没有强引用时，键值对会被自动移除。
     */
    private static final ThreadLocal<Map<Class<? extends ChannelHandler>, Integer>> MASKS =
            ThreadLocal.withInitial(() -> new WeakHashMap<>(32));


    /**
     * 还记得AbstractChannelHandlerContext的构造函数中的this.executionMask=mask(handlerClass)代码吗？
     * 这意味着ChannelHandlerContext在初始化的时候就为其封装的ChannelHandler定义好了事件类型
     *
     * @param clazz
     * @return
     */
    static int mask(Class<? extends ChannelHandler> clazz) {
        //得到存储事件类型的map，key为ChannelHandler，value为其感兴趣的事件类型的总和
        Map<Class<? extends ChannelHandler>, Integer> cache = MASKS.get();
        Integer mask = cache.get(clazz);

        if (mask == null) {
            //如果为null，说明是第一次添加，那就计算出该handler感兴趣的事件类型
            mask = mask0(clazz);
            //还要添加到map中
            cache.put(clazz, mask);
        }
        return mask;
    }

    /**
     * 计算ChannelHandler感兴趣的事件类型
     *
     * @param handlerType
     * @return
     */
    private static int mask0(Class<? extends ChannelHandler> handlerType) {
        int mask = MASK_EXCEPTION_CAUGHT;
        try {
            //判断该handler是否继承自ChannelInboundHandler类或者实现了该接口，这一步可以判断该handler是入站处理器还是出站处理器
            if (ChannelInboundHandler.class.isAssignableFrom(handlerType)) {
                //如果该ChannelHandler是Inbound类型的，则先将inbound事件全部设置进掩码中
                // mask |= MASK_ALL_INBOUND就是 mask = mask | MASK_ALL_INBOUND
                mask |= MASK_ALL_INBOUND;
                //接下来就找看看该handler对那些事件不感兴趣，不感兴趣的，就从感兴趣的事件总和中除去
                //判断的标准就是产看该handler的每个方法上是否添加了@Skip注解，如果添加了该注解，则表示不感兴趣，那就用先取反然后&运算，把
                //该事件的值从事件总和中减去，具体逻辑可以去看看ChannelInboundHandlerAdapter类，该类中的所有方法都添加了@Skip注解
                //只ChannelInboundHandlerAdapter的子类实现的方法没有@Skip注解，就表示该handler对特定事件感兴趣
                //每一个事件其实代表的就是handler中对应的方法是否可以被调用
                if (isSkippable(handlerType, "channelRegistered", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_REGISTERED;
                }
                if (isSkippable(handlerType, "channelUnregistered", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_UNREGISTERED;
                }
                if (isSkippable(handlerType, "channelActive", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_ACTIVE;
                }
                if (isSkippable(handlerType, "channelInactive", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_INACTIVE;
                }
                if (isSkippable(handlerType, "channelRead", ChannelHandlerContext.class, Object.class)) {
                    mask &= ~MASK_CHANNEL_READ;
                }
                if (isSkippable(handlerType, "channelReadComplete", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_READ_COMPLETE;
                }
                if (isSkippable(handlerType, "channelWritabilityChanged", ChannelHandlerContext.class)) {
                    mask &= ~MASK_CHANNEL_WRITABILITY_CHANGED;
                }
                if (isSkippable(handlerType, "userEventTriggered", ChannelHandlerContext.class, Object.class)) {
                    mask &= ~MASK_USER_EVENT_TRIGGERED;
                }
            }
            //和上面逻辑相同，只不过变成了出站处理器
            if (ChannelOutboundHandler.class.isAssignableFrom(handlerType)) {
                mask |= MASK_ALL_OUTBOUND;
                if (isSkippable(handlerType, "bind", ChannelHandlerContext.class,
                        SocketAddress.class, ChannelPromise.class)) {
                    mask &= ~MASK_BIND;
                }
                if (isSkippable(handlerType, "connect", ChannelHandlerContext.class, SocketAddress.class,
                        SocketAddress.class, ChannelPromise.class)) {
                    mask &= ~MASK_CONNECT;
                }
                if (isSkippable(handlerType, "disconnect", ChannelHandlerContext.class, ChannelPromise.class)) {
                    mask &= ~MASK_DISCONNECT;
                }
                if (isSkippable(handlerType, "close", ChannelHandlerContext.class, ChannelPromise.class)) {
                    mask &= ~MASK_CLOSE;
                }
                if (isSkippable(handlerType, "deregister", ChannelHandlerContext.class, ChannelPromise.class)) {
                    mask &= ~MASK_DEREGISTER;
                }
                if (isSkippable(handlerType, "read", ChannelHandlerContext.class)) {
                    mask &= ~MASK_READ;
                }
                if (isSkippable(handlerType, "write", ChannelHandlerContext.class,
                        Object.class, ChannelPromise.class)) {
                    mask &= ~MASK_WRITE;
                }
                if (isSkippable(handlerType, "flush", ChannelHandlerContext.class)) {
                    mask &= ~MASK_FLUSH;
                }
            }

            if (isSkippable(handlerType, "exceptionCaught", ChannelHandlerContext.class, Throwable.class)) {
                mask &= ~MASK_EXCEPTION_CAUGHT;
            }
        } catch (Exception e) {
            // Should never reach here.
            //PlatformDependent.throwException(e);
        }
        return mask;
    }


    @SuppressWarnings("rawtypes")
    private static boolean isSkippable(
            final Class<?> handlerType, final String methodName, final Class<?>... paramTypes) throws Exception {
        return AccessController.doPrivileged((PrivilegedExceptionAction<Boolean>) () -> {
            Method m;
            try {
                //判断该handler中是否实现了了对应事件的方法
                m = handlerType.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                log.debug(
                        "Class {} missing method {}, assume we can not skip execution", handlerType, methodName, e);
                return false;
            }
            //该方法不为null并且方法上有@Skip注解，表明对此事件不感兴趣
            return m != null && m.isAnnotationPresent(Skip.class);
        });
    }

    /**
     * Netty中的@Skip注解是用于标记跳过某些方法的注解。它通常用于在编写Netty的处理器（Handler）时，
     * 指定某些方法不参与处理逻辑，从而提高代码的可读性和性能。
     * 通过在方法上添加@Skip注解，Netty框架会跳过该方法的执行，
     * 不会触发相应的事件处理。这在某些场景下可以避免不必要的处理开销，
     * 提高性能。需要注意的是，@Skip注解只能用于标记方法，不能用于标记类或其他元素。
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Skip {
        // no value
    }

}
