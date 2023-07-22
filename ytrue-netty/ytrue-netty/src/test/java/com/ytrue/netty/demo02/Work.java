package com.ytrue.netty.demo02;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ytrue
 * @date 2023-07-22 10:39
 * @description Work
 */
@Slf4j
@Getter
public class Work implements Runnable {

    private volatile boolean start;

    private SelectorProvider provider;

    private Selector selector;

    private Thread thread;

    // private SelectionKey selectionKey;

    //private SocketChannel socketChannel;


    public Work() {
        provider = SelectorProvider.provider();
        this.selector = openSelector();
        thread = new Thread(this);
    }


    public void start() {
        if (start) {
            return;
        }
        start = true;
        thread.start();
    }

    public void register(SocketChannel socketChannel) {
        // this.socketChannel = socketChannel;
        try {
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Selector openSelector() {
        try {
            return provider.openSelector();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isReadable()) {
                        try {
                            SocketChannel socketChannel = (SocketChannel) key.channel();
                            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

                            if (socketChannel.read(byteBuffer) == -1) {
                                //socketChannel.close();
                                key.cancel();
                            }
                            byteBuffer.flip();
                            String str = Charset.forName("UTF-8").decode(byteBuffer).toString();
                            log.info("我是服务端：获取客户端发送的数据内容：{}", str);
                        } catch (Exception exception) {
                            log.error(exception.getMessage());
                            key.cancel();
                        }

                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
