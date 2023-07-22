package com.ytrue.netty.demo01;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * @author ytrue
 * @date 2023-07-22 9:32
 * @description NioSimpleServer
 */
@Slf4j
public class NioSimpleServer {

    public static void main(String[] args) throws IOException {

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        // 设置非阻塞
        serverSocketChannel.configureBlocking(false);
        // 创建选择器
        Selector selector = Selector.open();
        // 注册
        SelectionKey selectionKey = serverSocketChannel.register(selector, 0, null);
        selectionKey.interestOps(SelectionKey.OP_ACCEPT);
        serverSocketChannel.bind(new InetSocketAddress(9992));
        log.info("我是服务端：服务端启动成功");

        while (true) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if (key.isAcceptable()) {
                    ServerSocketChannel ssl = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = ssl.accept();
                    socketChannel.configureBlocking(false);
                    log.info("我是服务端：客户端连接建立成功");
                    socketChannel.register(selector, SelectionKey.OP_READ, null);

                    String str = "你好呀，你建立连接成功了";
                    ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(str);
                    socketChannel.write(byteBuffer);
                    log.info("我是服务端：客户端连接建立成功，发送数据成功");
                }

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

        }


    }
}
