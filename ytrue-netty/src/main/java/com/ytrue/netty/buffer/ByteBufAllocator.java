package com.ytrue.netty.buffer;

/**
 * @author ytrue
 * @date 2023-08-07 11:30
 * @description 内存分配器的顶级接口
 */
public interface ByteBufAllocator {


    ByteBufAllocator DEFAULT = ByteBufUtil.DEFAULT_ALLOCATOR;


    ByteBuf buffer();


    ByteBuf buffer(int initialCapacity);


    ByteBuf buffer(int initialCapacity, int maxCapacity);


    ByteBuf ioBuffer();


    ByteBuf ioBuffer(int initialCapacity);


    ByteBuf ioBuffer(int initialCapacity, int maxCapacity);


    ByteBuf heapBuffer();


    ByteBuf heapBuffer(int initialCapacity);


    ByteBuf heapBuffer(int initialCapacity, int maxCapacity);


    ByteBuf directBuffer();


    ByteBuf directBuffer(int initialCapacity);


    ByteBuf directBuffer(int initialCapacity, int maxCapacity);


    /**
     * @Author: ytrue
     * @Description:下面这几个方法暂且注释掉，下节课具体讲解ByteBuf的时候再引入
     */
//    CompositeByteBuf compositeBuffer();
//
//
//    CompositeByteBuf compositeBuffer(int maxNumComponents);
//
//
//    CompositeByteBuf compositeHeapBuffer();
//
//
//    CompositeByteBuf compositeHeapBuffer(int maxNumComponents);
//
//
//    CompositeByteBuf compositeDirectBuffer();
//
//
//    CompositeByteBuf compositeDirectBuffer(int maxNumComponents);


    boolean isDirectBufferPooled();


    int calculateNewCapacity(int minNewCapacity, int maxCapacity);
}

