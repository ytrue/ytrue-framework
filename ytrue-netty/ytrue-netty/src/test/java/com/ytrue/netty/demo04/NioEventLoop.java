package com.ytrue.netty.demo04;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * @author ytrue
 * @date 2023-07-22 13:07
 * @description NioEventLoop
 */
@Slf4j
public class NioEventLoop extends SingleThreadEventLoop {

    private final SelectorProvider provider;

    private Selector selector;

    public Selector selector() {
        return selector;
    }

    public NioEventLoop() {
        provider = SelectorProvider.provider();
        selector = openSelector();
    }


    /**
     * 创建选择器
     *
     * @return
     */
    private Selector openSelector() {
        try {
            return provider.openSelector();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void run() {
        while (true) {
            try {
                select();
                processSelectedKeys(selector.selectedKeys());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                runAllTasks();
            }
        }
    }


    private void select() throws IOException {
        Selector selector1 = this.selector;
        while (true) {
            int selectKeys = selector1.select(3000);
            if (selectKeys != 0 || hasTask()) {
                break;
            }
        }
    }

    private void processSelectedKeys(Set<SelectionKey> selectedKeys) {
        if (selectedKeys.isEmpty()) {
            return;
        }

        Iterator<SelectionKey> iterator = selectedKeys.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            processSelectedKeys(key);
        }
    }

    private void processSelectedKeys(SelectionKey key) {
        if (key.isReadable()) {
            try {
                SocketChannel socketChannel = (SocketChannel) key.channel();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

                if (socketChannel.read(byteBuffer) == -1) {
                    //socketChannel.close();
                    key.cancel();
                }
                byteBuffer.flip();
                String str = StandardCharsets.UTF_8.decode(byteBuffer).toString();
                log.info("我是服务端：获取客户端发送的数据内容：{}", str);
            } catch (Exception exception) {
                log.error(exception.getMessage());
                key.cancel();
            }
        }
    }
}
