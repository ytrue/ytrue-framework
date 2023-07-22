package com.ytrue.netty.demo04;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author ytrue
 * @date 2023-07-22 13:05
 * @description SingleThreadEventLoop
 */
@Slf4j
public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor {

    public void register(SocketChannel socketChannel, NioEventLoop nioEventLoop) {
        // 如果执行该方法的线程就是执行器中的线程，直接执行方法即可
        if (inEventLoop(Thread.currentThread())) {
            register0(socketChannel, nioEventLoop);
        } else {
            // /在这里，第一次向单线程执行器中提交任务的时候，执行器终于开始执行了,新的线程也开始创建
            nioEventLoop.execute(() -> register0(socketChannel, nioEventLoop));
        }
    }

    private void register0(SocketChannel socketChannel, NioEventLoop nioEventLoop) {
        try {
            socketChannel.configureBlocking(false);
            socketChannel.register(nioEventLoop.selector(), SelectionKey.OP_READ);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
