package com.ytrue.netty.channel.nio;

import com.ytrue.netty.channel.Channel;

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * @author ytrue
 * @date 2023-07-26 10:29
 * @description AbstractNioByteChannel
 * <p>
 * AbstractNioByteChannel是Netty中基于NIO的字节流通道的抽象类，它继承自AbstractNioChannel。AbstractNioByteChannel的作用主要有以下几点：
 * 1. 实现基于NIO的字节流通道：AbstractNioByteChannel实现了基于NIO的字节流通道的一些公共功能和行为。它通过底层的NIO Selector来管理通道的IO事件，处理读写操作等。
 * 2. 提供字节流的读写操作：AbstractNioByteChannel提供了字节流的读写操作方法，如read()、write()等。这些方法使用底层的NIO通道进行实际的读写操作，实现了字节流的传输。
 * 3. 处理读写操作的就绪事件：AbstractNioByteChannel通过重写AbstractNioChannel中的模板方法，处理具体的NIO事件，如读就绪、写就绪等。它将就绪的事件转发给ChannelPipeline中的下一个处理器进行处理。
 * 4. 管理字节流的缓冲区：AbstractNioByteChannel管理字节流的读写缓冲区。它通过ByteBuf来进行数据的读取和写入，提供了一些辅助方法来管理缓冲区的状态和操作。
 * 5. 支持零拷贝优化：AbstractNioByteChannel通过使用零拷贝技术，可以在数据传输时避免不必要的内存拷贝操作，提高了数据传输的效率。
 * 总的来说，AbstractNioByteChannel是Netty中用于实现基于NIO的字节流通道的抽象类。它提供了字节流的读写操作，处理读写操作的就绪事件，管理字节流的缓冲区，并支持零拷贝优化。具体的NIO字节流通道实现类可以继承AbstractNioByteChannel，并根据具体需求进行定制。
 */
public abstract class AbstractNioByteChannel extends AbstractNioChannel {

    public AbstractNioByteChannel(Channel parent, SelectableChannel ch) {
        super(parent, ch, SelectionKey.OP_READ);
    }

    /**
     * 由于还未引入unsafe类，所以该方法直接定义在这里，先不设定接口
     * 这个是客户端channel读取数据的方法
     */
    @Override
    protected void read() {
        //暂时用最原始简陋的方法处理
        ByteBuffer byteBuf = ByteBuffer.allocate(1024);
        try {
            doReadBytes(byteBuf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract int doReadBytes(ByteBuffer buf) throws Exception;
}
