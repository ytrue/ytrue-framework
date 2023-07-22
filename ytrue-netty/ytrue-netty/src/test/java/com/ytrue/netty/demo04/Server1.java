package com.ytrue.netty.demo04;

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
 * @date 2023-07-22 13:15
 * @description Server
 */
@Slf4j
public class Server1 {
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

        //初始化NioEventLoop数组
        NioEventLoop[] workGroup = new NioEventLoop[2];
        workGroup[0] = new NioEventLoop();
        workGroup[1] = new NioEventLoop();
        int i = 0;

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
                    log.info("我是服务端：客户端连接建立成功");

                    //计算要取值的数组的下表
                    int index = i % workGroup.length;
                    //把客户端的channel注册到新线程的selector上
                    workGroup[index].register(socketChannel,workGroup[index]);
                    i++;

                    String str = "你好呀，你建立连接成功了";
                    ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(str);
                    socketChannel.write(byteBuffer);
                    log.info("我是服务端：客户端连接建立成功，发送数据成功");
                }
            }
        }
    }
}
