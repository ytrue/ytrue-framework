package com.ytrue.netty.channel.socket.nio;

import com.ytrue.netty.channel.ChannelMetadata;
import com.ytrue.netty.channel.ChannelOption;
import com.ytrue.netty.channel.nio.AbstractNioMessageChannel;
import com.ytrue.netty.channel.nio.NioEventLoop;
import com.ytrue.netty.channel.socket.DefaultServerSocketChannelConfig;
import com.ytrue.netty.channel.socket.ServerSocketChannelConfig;
import com.ytrue.netty.util.internal.SocketUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.Map;

/**
 * @author ytrue
 * @date 2023-07-26 10:45
 * @description 对serversocketchannel做了一层包装，同时也因为channel接口和抽象类的引入，终于可以使NioEventLoop和channel解耦了
 */
@Slf4j
public class NioServerSocketChannel extends AbstractNioMessageChannel {

    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);

    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();

    private final ServerSocketChannelConfig config;

    /**
     * 通过SelectorProvider创建ServerSocketChannel
     *
     * @param provider
     * @return
     */
    private static ServerSocketChannel newSocket(SelectorProvider provider) {
        try {
            return provider.openServerSocketChannel();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open a server socket.", e);
        }
    }

    /**
     * 无参构造，当调用该构造器的时候，会调用到静态方法newSocket，返回一个ServerSocketChannel
     */
    public NioServerSocketChannel() {
        this(newSocket(DEFAULT_SELECTOR_PROVIDER));
    }

    public NioServerSocketChannel(ServerSocketChannel channel) {
        // 创建的为NioServerSocketChannel时，没有父类channel，SelectionKey.OP_ACCEPT是服务端channel的关注事件
        super(null, channel, SelectionKey.OP_ACCEPT);
        config = new NioServerSocketChannelConfig(this, javaChannel().socket());
    }


    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        //这里是一个系统调用方法，判断当前的java版本是否为7以上，这里我就直接写死了，不引入更多的工具类了
        //如果用户没有设置backlog参数，config.getBacklog()点进去看源码最后发现会该值会在NetUtil的静态代码块中被赋值，windows环境下值为200
        //linux环境下默认为128。Backlog可以设置全连接队列的大小，控制服务端接受连接的数量。
        //这里就直接写死了，还没有引入channelconfig配置类
        javaChannel().bind(localAddress, config.getBacklog());

        log.info("NioServerSocketChannel#doBind 设置全连接队列大小为：{}", config.getBacklog());
        if (isActive()) {
            log.info("服务端绑定端口成功");
            doBeginRead();
        }
    }

    @Override
    public boolean isActive() {
        return isOpen() && javaChannel().socket().isBound();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        //有连接进来，创建出java原生的客户端channel
        SocketChannel ch = SocketUtils.accept(javaChannel());
        try {
            if (ch != null) {
                //创建niosocketchannel
                buf.add(new NioSocketChannel(this, ch));
                return 1;
            }
        } catch (Throwable t) {
            t.printStackTrace();
            try {
                //有异常则关闭客户端的channel
                ch.close();
            } catch (Throwable t2) {
                throw new RuntimeException("Failed to close a socket.", t2);
            }
        }
        return 0;
    }

    @Override
    protected ServerSocketChannel javaChannel() {
        return (ServerSocketChannel) super.javaChannel();
    }

    @Override
    public NioEventLoop eventLoop() {
        return (NioEventLoop) super.eventLoop();
    }

    @Override
    protected SocketAddress localAddress0() {
        return SocketUtils.localSocketAddress(javaChannel().socket());
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null;
    }


    @Override
    protected void doFinishConnect() throws Exception {
        throw new UnsupportedOperationException();
    }


    @Override
    public ServerSocketChannelConfig config() {
        return config;
    }

    /**
     * 这里做空实现即可，服务端的channel并不会做连接动作
     *
     * @param remoteAddress
     * @param localAddress
     * @return
     * @throws Exception
     */
    @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doClose() throws Exception {
        javaChannel().close();
    }


    /**
     * 引入该内部类，该内部类最终会把用户配置的channel参数真正传入jdk的channel中
     * NioSocketChannel那边同理
     */
    private final class NioServerSocketChannelConfig extends DefaultServerSocketChannelConfig {
        private NioServerSocketChannelConfig(NioServerSocketChannel channel, ServerSocket javaSocket) {
            super(channel, javaSocket);
        }


        @Override
        public <T> boolean setOption(ChannelOption<T> option, T value) {
            if (option instanceof NioChannelOption) {
                //把用户设置的参数传入原生的jdk的channel中
                return NioChannelOption.setOption(jdkChannel(), (NioChannelOption<T>) option, value);
            }
            return super.setOption(option, value);
        }

        @Override
        public <T> T getOption(ChannelOption<T> option) {
            //这里有一行代码，判断jdk版本是否大于7，我就直接删掉了，默认大家用的都是7以上，否则要引入更多工具类
            if (option instanceof NioChannelOption) {
                return NioChannelOption.getOption(jdkChannel(), (NioChannelOption<T>) option);
            }
            return super.getOption(option);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map<ChannelOption<?>, Object> getOptions() {
            return getOptions(super.getOptions(), NioChannelOption.getOptions(jdkChannel()));
        }

        /**
         * 这个方法得到的就是jdk的channel
         *
         * @return
         */
        private ServerSocketChannel jdkChannel() {
            return ((NioServerSocketChannel) channel).javaChannel();
        }
    }

}
