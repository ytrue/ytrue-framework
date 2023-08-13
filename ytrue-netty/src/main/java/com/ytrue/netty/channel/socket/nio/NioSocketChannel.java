package com.ytrue.netty.channel.socket.nio;

import com.ytrue.netty.buffer.ByteBuf;
import com.ytrue.netty.channel.*;
import com.ytrue.netty.channel.nio.AbstractNioByteChannel;
import com.ytrue.netty.channel.socket.DefaultSocketChannelConfig;
import com.ytrue.netty.channel.socket.SocketChannelConfig;
import com.ytrue.netty.util.internal.SocketUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Map;

import static com.ytrue.netty.util.internal.ChannelUtils.MAX_BYTES_PER_GATHERING_WRITE_ATTEMPTED_LOW_THRESHOLD;

/**
 * @author ytrue
 * @date 2023-07-26 10:37
 * @description 对socketchannel做了一层包装，同时也因为channel接口和抽象类的引入，终于可以使NioEventLoop和channel解耦了
 */
@Slf4j
public class NioSocketChannel extends AbstractNioByteChannel {

    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();

    private final SocketChannelConfig config;

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
        config = new NioSocketChannelConfig(this, socket.socket());
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
        // 调用socketChannel.bind(address);
        SocketUtils.bind(javaChannel(), localAddress);
    }


    @Override
    public SocketChannelConfig config() {
        return config;
    }

    @Override
    public boolean isActive() {
        // channel是否为Connected状态，是客户端channel判断是否激活的条件。
        SocketChannel ch = javaChannel();
        return ch.isOpen() && ch.isConnected();
    }

    @Override
    protected int doReadBytes(ByteBuf byteBuf) throws Exception {
        //得到动态内存分配器的处理器
        final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();

        //这里先得到ByteBuf的可写字节数，然后将这个可写字节数赋值给处理器中的attemptedBytesRead属性
        //为什么要这么做？因为最后读取到的字节数和这个可以入的字节数相等了，说明这次读取数据已经满了，ByteBuf已经装不下数据了
        //但是这并不意味着channel中就没有可读取的数据了，这只能说明这个ByteBuf没办法再写入数据了

        //如果是另一种结果，就是最后读取到的字节数小于这个可写入的字节数，说明channel中的数据已经全部读取完了
        //总之，这个属性被赋值了，就可以很容易判断出读取了之后，客户端channel中是否还有数据可以被读取
        //这个byteBuf.writableBytes()的可写入字节数每次都是会变化的，这个要弄清楚
        allocHandle.attemptedBytesRead(byteBuf.writableBytes());

        //在这里把客户端channel和可写的字节数传进方法内，数据是要从客户端channel中写入到ByteBuf中的
        //这里就会把数据从channel写到ByteBuf中了
        return byteBuf.writeBytes(javaChannel(), allocHandle.attemptedBytesRead());
    }

    /**
     * @Author:ytrue
     * @Description:该方法就终于把数据从ByteBuf中发送到socket缓冲区中了
     * 使用的是ByteBuf
     */
    @Override
    protected int doWriteBytes(ByteBuf buf) throws Exception {
        //得到要发送的字节大小
        final int expectedWrittenBytes = buf.readableBytes();
        //发送到SocketChannel中
        return buf.readBytes(javaChannel(), expectedWrittenBytes);
    }

    /**
     * @Author:ytrue
     * @Description:这个就是用零拷贝的方式传输文件
     */
    @Override
    protected long doWriteFileRegion(FileRegion region) throws Exception {
        final long position = region.transferred();
        //零拷贝的方法传输数据
        return region.transferTo(javaChannel(), position);
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

            /*
             * 人为将程序中止，等待连接创建完成,否则会报如下错误
             * java.nio.channels.NotYetConnectedException at sun.nio.ch.SocketChannelImpl.ensureWriteOpen(SocketChannelImpl.java:274)
             */
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


    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) super.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) super.remoteAddress();
    }



    @Override
    protected SocketAddress localAddress0() {
        return javaChannel().socket().getLocalSocketAddress();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return javaChannel().socket().getRemoteSocketAddress();
    }


    @Override
    protected void doFinishConnect() throws Exception {
        if (!javaChannel().finishConnect()) {
            throw new Error();
        }
        // 看，在这里就能拿到socketchannel并且发送消息成功！说明问题并不是出在连接上，一定是哪里没有阻塞住，仔细想想netty的线程模型
//        SocketChannel socketChannel = javaChannel();
//        socketChannel.write(ByteBuffer.wrap("我是真正的netty！".getBytes()));
//        System.out.println("数据发送了！");
    }


    /**
     * 关闭channel
     *
     * @throws Exception
     */
    protected void doClose() throws Exception {
        javaChannel().close();
    }



    /**
     * @Author:ytrue
     * @Description:重构之后的doWrite方法，也就是把消息真正刷新到socket中的方法
     */
    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        //得到SocketChannel，这个是java的nio中原生channel
        SocketChannel ch = javaChannel();
        //这里会得到写数据的最大次数，默认为16次。限定这个16次的原因和read方法的接收数据和连接的原因一样
        //都是为了均衡单线程执行器的工作对象。因为单线程执行器管理了多个channel，还要执行用户提交的各种任务
        //不能把单线程执行器都用在一个channel上
        //当然，这个参数也是可以通过配置修改的。就是通过ChannelOption.WRITE_SPIN_COUNT来修改
        int writeSpinCount = config().getWriteSpinCount();

        do {
            //判断写缓冲区是否为空，因为要从写缓冲区中把数据刷新到socket中
            if (in.isEmpty()) {
                //这个方法的作用是把监听的write事件从多路复用器上取消掉
                //为什么要这么做呢？因为写缓冲区已经为空了，说明写缓冲区已经可以继续存放消息数据了，这就意味着不必再
                //注册write事件。注意哦，这里要再次理清楚，什么时候注册write事件？只有当socket缓冲区不可写的时候
                //才要注册write事件，然后多路复用器检测到write事件，然后执行 ch.unsafe().forceFlush()方法
                //把消息异步flush到socket中。这里也可以看出来write事件触发后其实执行的是flush方法
                //但现在写缓冲区为null
                //说明没有数据可以flush，所以就不必再注册write事件了
                //否则selector会一直检测到该事件，这就是白费功夫了，所以这里要移除write事件
                //当然，这里大家可能还不明白write事件在哪里被注册到多路复用器上了，下面会看到具体逻辑的。
                clearOpWrite();
                //直接返回即可，注意，这里直接返回了是因为写缓冲区为null，没有消息可以被刷新到socket中，所以直接返回了
                //到此为止，其实大家应该明白一点，肯定是在下面的逻辑中达到某个限制了，不能再
                return;
            }
            //这里得到的是一个默认的可以发送消息的最大字节数，并且这个最大字节数是可以动态调整的
            int maxBytesPerGatheringWrite = ((NioSocketChannelConfig) config).getMaxBytesPerGatheringWrite();

            //这里是要把写缓冲区中存放的待刷新消息转换成ByteBuffer，如果消息有很多，就把它们转换成ByteBuffer数组
            //实际上就是把ByteBuf转换成ByteBuffer，为什么要这么做呢？再往下看大家就会明白了
            //因为说到底Netty是建立在nio之上的一个框架，底层发送消息使用的仍然是nio的数据结构，也就是ByteBuffer，向
            //channel中写入数据使用的就是ByteBuffer，所以，要把ByteBuf转换成ByteBuffer
            //该方法中的两个参数含义其实很明显，因为要把ByteBuf转换成ByteBuffer数组，1024指的就是这次发送消息，最多可以转换1024个ByteBuffer
            //也就是说，nioBuffers数组的长度最大为1024，maxBytesPerGatheringWrite就是本次发送消息所能发送的最大字节数
            ByteBuffer[] nioBuffers = in.nioBuffers(1024, maxBytesPerGatheringWrite);

            //这里得到了要发送的ByteBuffer的个数
            int nioBufferCnt = in.nioBufferCount();

            switch (nioBufferCnt) {
                case 0:
                    //个数为0的时候，意味着没有要发送的消息，但是并不是意味着没有要发送的数据
                    //其实零拷贝就是在这个地方操作的
                    writeSpinCount -= doWrite0(in);
                    break;
                case 1: {
                    //如果个数为1，说明只发送一个ByteBuffer，所以才有了取nioBuffers数组的0号索引位置的数据
                    ByteBuffer buffer = nioBuffers[0];
                    //这里得到ByteBuffer中要发送出去的字节大小
                    int attemptedBytes = buffer.remaining();
                    //这里就把ByteBuffer中的数据写到channel中了，这里使用的是nio中的原生方法
                    //这里返回的这个值就是写到channel中的字节大小
                    //注意，下面有一个if分支，判断这个返回值是正是负，这里就要解释一下了，如果ch.write(buffer)方法的返回值为-1
                    //就说明socket缓冲区已经满了，不能再把消息向里面发送了
                    //这个返回值的几种情况大家可以去查一查，其实这些方法往下调用都会调用到本地方法，然后是C++的方法，应该是这样的
                    final int localWrittenBytes = ch.write(buffer);
                    //这里判断上面的返回值是不是小于等于0的，如果是说明socket缓冲区已经满了
                    //不能再继续写入数据，这时候就要注册write事件，而该事件会在socket可写时被触发，然后会通过unsafe调用flush方法
                    //继续刷新消息到socket中
                    if (localWrittenBytes <= 0) {
                        //这里就是注册write事件的方法
                        //注意，这里注册的write事件的情况，是在写次数没有达到16次的情况下，socket缓冲区就满了，不可写了，这时候注册了一个
                        //write事件，有这种情况，就有另一种情况，那就是达到16次的写次数了，但是socket还没满，还是可写的，这时候该怎么办呢？
                        //实际上也会调用这个方法，只不过参数由true改成了false，具体逻辑，可以去该方法内查看
                        incompleteWrite(true);
                        return;
                    }
                    //走到这里说明localWrittenBytes是大于0的，也就是发送消息成功的意思
                    //下面这个方法就是根据已发送的字节数，调整下一次可以发送的消息字节的数量
                    adjustMaxBytesPerGatheringWrite(attemptedBytes, localWrittenBytes, maxBytesPerGatheringWrite);
                    //已经从写缓冲区中刷新了这么多字节了，所以要把这些字节从写缓冲区中删除了
                    in.removeBytes(localWrittenBytes);

                    //写次数减1
                    --writeSpinCount;
                    break;
                }
                default: {
                    //走到这里说明要发送的不是单个ByteBuffer，是一个ByteBuffer数组
                    //这里得到要发送的总的ByteBuffer的字节大小
                    long attemptedBytes = in.nioBufferSize();
                    //开始刷新消息到socket中
                    final long localWrittenBytes = ch.write(nioBuffers, 0, nioBufferCnt);
                    //下面的逻辑同上面一样，就不再重复了注释了
                    if (localWrittenBytes <= 0) {
                        incompleteWrite(true);
                        return;
                    }
                    adjustMaxBytesPerGatheringWrite((int) attemptedBytes, (int) localWrittenBytes,
                            maxBytesPerGatheringWrite);
                    in.removeBytes(localWrittenBytes);
                    --writeSpinCount;
                    break;
                }
            }
        } while (writeSpinCount > 0);
        //走到这里说明已经退出循环了，但是退出循环的时候也分几种情况，这里指的是writeSpinCount这个值
        //这个值在最初是16次，退出循环的时候有可能正好是等于0，这意味着已经写了16次了，
        //但是请大家注意，如果缓冲区直接不可写了，或者数据消息发送完了，比如上面的in.isEmpty()方法为true，就会直接return，退出整个方法，就不会走到这里了
        //走到这里意味着缓冲区还可写，但是已经写完16次了可是还有数据没有刷新到缓冲区
        //所以就判断writeSpinCount < 0，这时候writeSpinCount为0，所以返回false，所以在下面的方法内就会直接封装一个异步任务，在
        //异步任务中刷新消息到socket中
        //但是，还有另一种情况，就是在doWrite0(in)方法中，大家还记得这个方法会返回一个很大的整数值，然后让writeSpinCount减去这个值
        //得到一个负数吗？如果是这种情况，就说明socket满了，需要注册write事件。所以writeSpinCount < 0为true，那就会在下面的方法
        //中注册write事件
        incompleteWrite(writeSpinCount < 0);
    }

    /**
     * @Author:ytrue
     * @Description:动态调节下次可以发送的字节数的最大值
     */
    private void adjustMaxBytesPerGatheringWrite(int attempted, int written, int oldMaxBytesPerGatheringWrite) {
        //如果本次写入的字节数等于一开始期望的数值
        if (attempted == written) {
            //就把下次的可以写socket的字节数扩大一倍
            if (attempted << 1 > oldMaxBytesPerGatheringWrite) {
                ((NioSocketChannelConfig) config).setMaxBytesPerGatheringWrite(attempted << 1);
            }
            //written < attempted >>> 1这个判断就是本次写入的字节比期望写入的二分之一还小
            //这可能就意味着socket中没有容量了，写不了太多了
            //attempted > MAX_BYTES_PER_GATHERING_WRITE_ATTEMPTED_LOW_THRESHOLD这个就是要求写入的字节数不能小于4096
            //这时候就把下次可写的字节数减少一倍
        } else if (attempted > MAX_BYTES_PER_GATHERING_WRITE_ATTEMPTED_LOW_THRESHOLD && written < attempted >>> 1) {
            ((NioSocketChannelConfig) config).setMaxBytesPerGatheringWrite(attempted >>> 1);
        }
    }

    /**
     * 用户设置的客户端channel的参数由此类进行设置，这里面有的方法现在还不需要是用来干什么的
     */
    private final class NioSocketChannelConfig extends DefaultSocketChannelConfig {
        private volatile int maxBytesPerGatheringWrite = Integer.MAX_VALUE;

        private NioSocketChannelConfig(NioSocketChannel channel, Socket javaSocket) {
            super(channel, javaSocket);
            calculateMaxBytesPerGatheringWrite();
        }

        @Override
        public NioSocketChannelConfig setSendBufferSize(int sendBufferSize) {
            super.setSendBufferSize(sendBufferSize);
            calculateMaxBytesPerGatheringWrite();
            return this;
        }

        @Override
        public <T> boolean setOption(ChannelOption<T> option, T value) {
            if (option instanceof NioChannelOption) {
                return NioChannelOption.setOption(jdkChannel(), (NioChannelOption<T>) option, value);
            }
            return super.setOption(option, value);
        }

        @Override
        public <T> T getOption(ChannelOption<T> option) {
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

        void setMaxBytesPerGatheringWrite(int maxBytesPerGatheringWrite) {
            this.maxBytesPerGatheringWrite = maxBytesPerGatheringWrite;
        }

        int getMaxBytesPerGatheringWrite() {
            return maxBytesPerGatheringWrite;
        }

        private void calculateMaxBytesPerGatheringWrite() {
            int newSendBufferSize = getSendBufferSize() << 1;
            if (newSendBufferSize > 0) {
                setMaxBytesPerGatheringWrite(getSendBufferSize() << 1);
            }
        }

        private SocketChannel jdkChannel() {
            return ((NioSocketChannel) channel).javaChannel();
        }
    }
}
