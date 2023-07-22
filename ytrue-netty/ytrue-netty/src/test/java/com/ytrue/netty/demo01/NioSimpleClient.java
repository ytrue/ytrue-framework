package com.ytrue.netty.demo01;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * @author ytrue
 * @date 2023-07-22 10:11
 * @description NioSimpleClient
 */
@Slf4j
public class NioSimpleClient {

    public static void main(String[] args) throws IOException {

        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        SelectionKey selectionKey = socketChannel.register(selector, 0, null);
        selectionKey.interestOps(SelectionKey.OP_CONNECT);
        socketChannel.connect(new InetSocketAddress(9992));

        while (true) {
            selector.select();

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (key.isConnectable()) {
                    if (socketChannel.finishConnect()) {
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        String str = "我是客户端：我连接上来了";
                        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(str);
                        socketChannel.write(byteBuffer);
                        log.info("我是客户端：客户端发送数据");
                    }
                }

                if (key.isReadable()) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    if (socketChannel.read(byteBuffer) == -1) {
                        //socketChannel.close();
                        key.cancel();
                    }

                    byteBuffer.flip();
                    String str = Charset.forName("UTF-8").decode(byteBuffer).toString();
                    log.info("我是客户端：获取服务端发送的数据内容：{}", str);
                }

            }


        }

    }
}
