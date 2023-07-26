package com.ytrue.netty.channel.socket;

import com.ytrue.netty.channel.Channel;
import com.ytrue.netty.channel.nio.AbstractNioByteChannel;
import com.ytrue.netty.util.internal.SocketUtils;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * @author ytrue
 * @date 2023-07-26 10:37
 * @description 对socketchannel做了一层包装，同时也因为channel接口和抽象类的引入，终于可以使NioEventLoop和channel解耦了
 */
public class NioSocketChannel extends AbstractNioByteChannel {

    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();


    /**
     * 通过SelectorProvider获取SocketChannel
     *
     * @param provider
     * @return
     */
    private static SocketChannel newSocket(SelectorProvider provider) {
        try {
            // 创建SocketChannel
            return provider.openSocketChannel();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open a socket.", e);
        }
    }

    /**
     * 无参构造，当调用该构造器的时候，会调用到静态方法newSocket，返回一个SocketChannel
     */
    public NioSocketChannel() {
        this(DEFAULT_SELECTOR_PROVIDER);
    }

    public NioSocketChannel(SelectorProvider provider) {
        this(newSocket(provider));
    }

    public NioSocketChannel(SocketChannel socket) {
        this(null, socket);
    }

    public NioSocketChannel(Channel parent, SocketChannel socket) {
        super(parent, socket);
    }

    @Override
    protected SocketChannel javaChannel() {
        return (SocketChannel) super.javaChannel();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        doBind0(localAddress);
    }

    /**
     * 这里是一个系统调用方法，判断当前的java版本是否为7以上，这里我就直接写死了，默认用的都是java8，不引入更多的工具类了
     *
     * @param localAddress
     * @throws Exception
     */
    private void doBind0(SocketAddress localAddress) throws Exception {
        //  socketChannel.bind(address);
        SocketUtils.bind(javaChannel(), localAddress);
    }

    @Override
    public boolean isActive() {
        // channel是否为Connected状态，是客户端channel判断是否激活的条件。
        SocketChannel ch = javaChannel();
        return ch.isOpen() && ch.isConnected();
    }

    @Override
    protected int doReadBytes(ByteBuffer byteBuf) throws Exception {
        // channel 读取到byteBuf中
        int len = javaChannel().read(byteBuf);
        byte[] buffer = new byte[len];
        // 设置读模式
        byteBuf.flip();
        byteBuf.get(buffer);
        System.out.println("客户端收到消息:{}" + new String(buffer));
        //返回读取到的字节长度
        return len;
    }

    @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        //从这里可以看出，如果连接的时候把本地地址也传入了，那么就要在连接远端的同时，监听本地端口号
        if (localAddress != null) {
            doBind0(localAddress);
        }
        boolean success = false;
        try {
            //SocketUtils.connect(javaChannel(), remoteAddress)该方法如果连接成功就直接返回true
            //如果没有接收到服务端的ack就会返回false，但并不意味着该方法就彻底失败了，有可能ack在路上等等，最终需要注册连接事件来监听结果
            //这会让AbstractNioChannel类中的connect方法进入到添加定时任务的分支，如果超过设定的时间一直没有连接成功，就会在客户端报错
            //如果连接成功了，连接成功的时候会把该定时任务中的某些变量置为null,现在我们还没有加入定时任务
            //这里会返回false
            boolean connected = SocketUtils.connect(javaChannel(), remoteAddress);
            if (!connected) {
                selectionKey().interestOps(SelectionKey.OP_CONNECT);
            }
            success = true;
            return connected;
        } finally {
            if (!success) {
                doClose();
            }
        }
    }


    /**
     * 关闭channel
     *
     * @throws Exception
     */
    protected void doClose() throws Exception {
        javaChannel().close();
    }

}
