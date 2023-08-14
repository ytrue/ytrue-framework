package com.ytrue.netty.util.internal;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ytrue
 * @date 2023-08-03 9:33
 * @description InternalThreadLocalMap的父类，定义了InternalThreadLocalMap存储数据的容器数组。其他的属性有些还用不到
 * 先列出来，等用到的时候再说
 * 注意，虽然InternalThreadLocalMap是以Map结尾的，但是它存储数据的容器实际上是个数组
 */
public class UnpaddedInternalThreadLocalMap {


    /**
     * 如果使用的线程不是fastthreadlocalthread，那就返回一个原生的ThreadLocal，原生的ThreadLocal可以得到原生的ThreadLocalMap
     */
    static final ThreadLocal<InternalThreadLocalMap> slowThreadLocalMap = new ThreadLocal<InternalThreadLocalMap>();
    /**
     * FastThreadLocal的索引，每个FastThreadLocal都会有一个索引，也就是要存放到数组的下标位置。该索引在FastThreadLocal创建的时候就
     * 初始化好了。是原子递增的
     */
    static final AtomicInteger nextIndex = new AtomicInteger();

    /**
     * 真正存放数据的数组，就是InternalThreadLocalMap存储数据的容器数组，这时候要注意一个区别，在原生threadlocalmap中，threadlocal会作为key
     * 存入到threadlocalmap中，而在Netty中，fastthreadlocal只会提供一个数组下标的索引，并不会存入数组中，放进数组中的是对应的value值
     */
    Object[] indexedVariables;


    int futureListenerStackDepth;
    int localChannelReaderStackDepth;
    Map<Class<?>, Boolean> handlerSharableCache;
    ThreadLocalRandom random;

    /**
     * 下面这两个属性暂时用不到，等到了编解码的课程时，我们再引入它们
     */
    Map<Class<?>, TypeParameterMatcher> typeParameterMatcherGetCache;
    Map<Class<?>, Map<String, TypeParameterMatcher>> typeParameterMatcherFindCache;


    StringBuilder stringBuilder;
    Map<Charset, CharsetEncoder> charsetEncoderCache;
    Map<Charset, CharsetDecoder> charsetDecoderCache;

    ArrayList<Object> arrayList;

    UnpaddedInternalThreadLocalMap(Object[] indexedVariables) {
        this.indexedVariables = indexedVariables;
    }
}
