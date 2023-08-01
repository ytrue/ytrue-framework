package com.ytrue.netty.handler.timeout;

/**
 * @author ytrue
 * @date 2023-08-01 9:01
 * @description IdleState
 */
public enum IdleState {

    /**
     * 读空闲
     */
    READER_IDLE,
    /**
     * 写空闲
     */
    WRITER_IDLE,
    /**
     * 读和写空闲
     */
    ALL_IDLE
}
