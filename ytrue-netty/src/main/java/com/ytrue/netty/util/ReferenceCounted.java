package com.ytrue.netty.util;

/**
 * @author ytrue
 * @date 2023-08-07 11:29
 * @description ReferenceCounted 计数器
 */
public interface ReferenceCounted {

    int refCnt();


    ReferenceCounted retain();


    ReferenceCounted retain(int increment);


    ReferenceCounted touch();

    ReferenceCounted touch(Object hint);


    boolean release();


    boolean release(int decrement);
}
