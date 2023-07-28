package com.ytrue.netty.channel;

import com.ytrue.netty.util.AbstractConstant;
import com.ytrue.netty.util.ConstantPool;

import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * @author ytrue
 * @date 2023-07-28 9:49
 * @description 我们真正使用的常量类，这里面我删除了一些方法，都是被作者废弃了的方法
 */
public class ChannelOption<T> extends AbstractConstant<ChannelOption<T>> {


    /**
     * 常量池
     */
    private static final ConstantPool<ChannelOption<Object>> pool = new ConstantPool<ChannelOption<Object>>() {
        // ConstantPool抽象类中的抽象方法，在这里得到了实现
        @Override
        protected ChannelOption<Object> newConstant(int id, String name) {
            return new ChannelOption<>(id, name);
        }
    };


    /**
     * ConstantPool的valueOf,存入到常量池，并且返回创建的参数
     *
     * @param name
     * @param <T>
     * @return
     */
    public static <T> ChannelOption<T> valueOf(String name) {
        return (ChannelOption<T>) pool.valueOf(name);
    }


    /**
     * ConstantPool的valueOf,存入到常量池，并且返回创建的参数
     *
     * @param firstNameComponent
     * @param secondNameComponent
     * @param <T>
     * @return
     */
    public static <T> ChannelOption<T> valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return (ChannelOption<T>) pool.valueOf(firstNameComponent, secondNameComponent);
    }


    /**
     * 判断key是否存在 常量池中
     *
     * @param name
     * @return
     */
    public static boolean exists(String name) {
        return pool.exists(name);
    }


    /**
     * 构造
     *
     * @param id
     * @param name
     */
    protected ChannelOption(int id, String name) {
        super(id, name);
    }

    @Deprecated
    protected ChannelOption(String name) {
        this(pool.nextId(), name);
    }


    /**
     * 校验，判断value是否为null
     * @param value
     */
    public void validate(T value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
    }


    /**
     * 在源码中，下面的这些属性都是作者已经创建好的常量，看看有没有你熟悉的
     */
    public static final ChannelOption<Integer> CONNECT_TIMEOUT_MILLIS = valueOf("CONNECT_TIMEOUT_MILLIS");
    public static final ChannelOption<Integer> WRITE_SPIN_COUNT = valueOf("WRITE_SPIN_COUNT");
    public static final ChannelOption<Boolean> ALLOW_HALF_CLOSURE = valueOf("ALLOW_HALF_CLOSURE");
    public static final ChannelOption<Boolean> AUTO_READ = valueOf("AUTO_READ");
    public static final ChannelOption<Boolean> AUTO_CLOSE = valueOf("AUTO_CLOSE");
    public static final ChannelOption<Boolean> SO_BROADCAST = valueOf("SO_BROADCAST");
    public static final ChannelOption<Boolean> SO_KEEPALIVE = valueOf("SO_KEEPALIVE");
    public static final ChannelOption<Integer> SO_SNDBUF = valueOf("SO_SNDBUF");
    public static final ChannelOption<Integer> SO_RCVBUF = valueOf("SO_RCVBUF");
    public static final ChannelOption<Boolean> SO_REUSEADDR = valueOf("SO_REUSEADDR");
    public static final ChannelOption<Integer> SO_LINGER = valueOf("SO_LINGER");


    /**
     * 记得我们给channel配置的参数吗option(ChannelOption.SO_BACKLOG,128)，是不是很熟悉，我们拿来即用的常量，因为作者
     * 已经创建好了。这里我多说一句，不要被ChannelOption<T>中的泛型给迷惑了，觉得ChannelOption中也存储着
     * 用户定义的值，就是那个泛型的值，比如说option(ChannelOption.SO_BACKLOG,128)里面的128，以为ChannelOption<Integer>中的integer
     * 存储的就是128，实际上128存储在serverbootstrap的linkmap中。而作者之所以给常量类设定泛型，是因为Attribut会存储泛型的值
     */
    public static final ChannelOption<Integer> SO_BACKLOG = valueOf("SO_BACKLOG");
    public static final ChannelOption<Integer> SO_TIMEOUT = valueOf("SO_TIMEOUT");
    public static final ChannelOption<Integer> IP_TOS = valueOf("IP_TOS");
    public static final ChannelOption<InetAddress> IP_MULTICAST_ADDR = valueOf("IP_MULTICAST_ADDR");
    public static final ChannelOption<NetworkInterface> IP_MULTICAST_IF = valueOf("IP_MULTICAST_IF");
    public static final ChannelOption<Integer> IP_MULTICAST_TTL = valueOf("IP_MULTICAST_TTL");
    public static final ChannelOption<Boolean> IP_MULTICAST_LOOP_DISABLED = valueOf("IP_MULTICAST_LOOP_DISABLED");
    public static final ChannelOption<Boolean> TCP_NODELAY = valueOf("TCP_NODELAY");
    public static final ChannelOption<Boolean> SINGLE_EVENTEXECUTOR_PER_GROUP = valueOf("SINGLE_EVENTEXECUTOR_PER_GROUP");


}
