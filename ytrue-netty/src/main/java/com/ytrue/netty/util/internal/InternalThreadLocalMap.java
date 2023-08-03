package com.ytrue.netty.util.internal;

import com.ytrue.netty.util.concurrent.FastThreadLocalThread;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.*;

/**
 * @author ytrue
 * @date 2023-08-03 11:00
 * @description 该类虽然名为map, 实际上是一个数组, 是要配合fastthreadlocal来使用的
 */
@Slf4j
public class InternalThreadLocalMap extends UnpaddedInternalThreadLocalMap {


    /**
     * 下面这三个属性暂时用不到，所以先不介绍了
     */
    private static final int DEFAULT_ARRAY_LIST_INITIAL_CAPACITY = 8;
    private static final int STRING_BUILDER_INITIAL_SIZE;
    private static final int STRING_BUILDER_MAX_SIZE;

    /**
     * 未定义的一个对象，起这个名字是因为，一旦线程私有的map中删掉了一个value，那空出来的位置就会被该对象赋值
     */
    public static final Object UNSET = new Object();

    private BitSet cleanerFlags;

    static {
        STRING_BUILDER_INITIAL_SIZE =
                SystemPropertyUtil.getInt("io.netty.threadLocalMap.stringBuilder.initialSize", 1024);
        log.debug("-Dio.netty.threadLocalMap.stringBuilder.initialSize: {}", STRING_BUILDER_INITIAL_SIZE);

        STRING_BUILDER_MAX_SIZE = SystemPropertyUtil.getInt("io.netty.threadLocalMap.stringBuilder.maxSize", 1024 * 4);
        log.debug("-Dio.netty.threadLocalMap.stringBuilder.maxSize: {}", STRING_BUILDER_MAX_SIZE);
    }


    /**
     * 返回本地map
     *
     * @return
     */
    public static InternalThreadLocalMap getIfSet() {
        //获得执行当前方法的线程
        Thread thread = Thread.currentThread();
        //判断该线程是否为fast体系的线程。因为只有被包装过的线程配合InternalThreadLocalMap才能发挥出高性能
        if (thread instanceof FastThreadLocalThread) {
            //返回InternalThreadLocalMap
            return ((FastThreadLocalThread) thread).threadLocalMap();
        }
        //这里是返回java原生的threadlocalmap。
        return slowThreadLocalMap.get();
    }


    /**
     * 返回一个InternalThreadLocalMap
     *
     * @return
     */
    public static InternalThreadLocalMap get() {
        Thread thread = Thread.currentThread();
        if (thread instanceof FastThreadLocalThread) {
            //返回InternalThreadLocalMap
            return fastGet((FastThreadLocalThread) thread);
        } else {
            //这里是返回java原生的threadlocalmap。
            return slowGet();
        }
    }


    /**
     * 得到InternalThreadLocalMap
     *
     * @param thread
     * @return
     */
    private static InternalThreadLocalMap fastGet(FastThreadLocalThread thread) {
        InternalThreadLocalMap threadLocalMap = thread.threadLocalMap();
        if (threadLocalMap == null) {
            thread.setThreadLocalMap(threadLocalMap = new InternalThreadLocalMap());
        }
        return threadLocalMap;
    }


    /**
     * 得到Java原生的本地map  ThreadLocalMap 里面存储了InternalThreadLocalMap
     *
     * @return
     */
    private static InternalThreadLocalMap slowGet() {
        ThreadLocal<InternalThreadLocalMap> slowThreadLocalMap = UnpaddedInternalThreadLocalMap.slowThreadLocalMap;
        InternalThreadLocalMap ret = slowThreadLocalMap.get();
        if (ret == null) {
            ret = new InternalThreadLocalMap();
            slowThreadLocalMap.set(ret);
        }
        return ret;
    }


    /**
     * 把线程的私有map置为null
     */
    public static void remove() {
        Thread thread = Thread.currentThread();
        if (thread instanceof FastThreadLocalThread) {
            ((FastThreadLocalThread) thread).setThreadLocalMap(null);
        } else {
            slowThreadLocalMap.remove();
        }
    }

    public static void destroy() {
        slowThreadLocalMap.remove();
    }


    /**
     * 该方法用来给fastthreadlocal的index赋值
     *
     * @return
     */
    public static int nextVariableIndex() {
        int index = nextIndex.getAndIncrement();
        if (index < 0) {
            nextIndex.decrementAndGet();
            throw new IllegalStateException("too many thread-local indexed variables");
        }
        return index;
    }

    public static int lastVariableIndex() {
        return nextIndex.get() - 1;
    }


    /**
     * Cache line padding (must be public)
     * With CompressedOops enabled, an instance of this class should occupy at least 128 bytes.
     * 填充字节用来解决伪共享问题，但是作用不大，新的Netty版本中被废弃了
     */
    public long rp1, rp2, rp3, rp4, rp5, rp6, rp7, rp8, rp9;

    private InternalThreadLocalMap() {
        super(newIndexedVariableTable());
    }


    /**
     * 初始化数组，该数组就是在map中存储数据用的
     *
     * @return
     */
    private static Object[] newIndexedVariableTable() {
        Object[] array = new Object[32];
        // 填充
        Arrays.fill(array, UNSET);
        return array;
    }


    /**
     * 得到该map存储元素的个数，这个方法内前面几个判断先别看，因为这里用不到，只看最后一个判断即可
     * 最后一个判断就是取数组里存储元素的个数
     *
     * @return
     */
    public int size() {
        int count = 0;

        if (futureListenerStackDepth != 0) {
            count++;
        }
        if (localChannelReaderStackDepth != 0) {
            count++;
        }
        if (handlerSharableCache != null) {
            count++;
        }
        if (random != null) {
            count++;
        }
//        if (typeParameterMatcherGetCache != null) {
//            count ++;
//        }
//        if (typeParameterMatcherFindCache != null) {
//            count ++;
//        }
        if (stringBuilder != null) {
            count++;
        }
        if (charsetEncoderCache != null) {
            count++;
        }
        if (charsetDecoderCache != null) {
            count++;
        }
        if (arrayList != null) {
            count++;
        }

        for (Object o : indexedVariables) {
            if (o != UNSET) {
                count++;
            }
        }

        // We should subtract 1 from the count because the first element in 'indexedVariables' is reserved
        // by 'FastThreadLocal' to keep the list of 'FastThreadLocal's to remove on 'FastThreadLocal.removeAll()'.
        return count - 1;
    }


    /**
     * 下面这几个方法也用不到
     *
     * @return
     */
    public StringBuilder stringBuilder() {
        StringBuilder sb = stringBuilder;
        if (sb == null) {
            return stringBuilder = new StringBuilder(STRING_BUILDER_INITIAL_SIZE);
        }
        if (sb.capacity() > STRING_BUILDER_MAX_SIZE) {
            sb.setLength(STRING_BUILDER_INITIAL_SIZE);
            sb.trimToSize();
        }
        sb.setLength(0);
        return sb;
    }

    public Map<Charset, CharsetEncoder> charsetEncoderCache() {
        Map<Charset, CharsetEncoder> cache = charsetEncoderCache;
        if (cache == null) {
            charsetEncoderCache = cache = new IdentityHashMap<Charset, CharsetEncoder>();
        }
        return cache;
    }

    public Map<Charset, CharsetDecoder> charsetDecoderCache() {
        Map<Charset, CharsetDecoder> cache = charsetDecoderCache;
        if (cache == null) {
            charsetDecoderCache = cache = new IdentityHashMap<Charset, CharsetDecoder>();
        }
        return cache;
    }

    public <E> ArrayList<E> arrayList() {
        return arrayList(DEFAULT_ARRAY_LIST_INITIAL_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public <E> ArrayList<E> arrayList(int minCapacity) {
        ArrayList<E> list = (ArrayList<E>) arrayList;
        if (list == null) {
            arrayList = new ArrayList<Object>(minCapacity);
            return (ArrayList<E>) arrayList;
        }
        list.clear();
        list.ensureCapacity(minCapacity);
        return list;
    }

    public int futureListenerStackDepth() {
        return futureListenerStackDepth;
    }

    public void setFutureListenerStackDepth(int futureListenerStackDepth) {
        this.futureListenerStackDepth = futureListenerStackDepth;
    }

    public ThreadLocalRandom random() {
        ThreadLocalRandom r = random;
        if (r == null) {
            random = r = new ThreadLocalRandom();
        }
        return r;
    }

//    public Map<Class<?>, TypeParameterMatcher> typeParameterMatcherGetCache() {
//        Map<Class<?>, TypeParameterMatcher> cache = typeParameterMatcherGetCache;
//        if (cache == null) {
//            typeParameterMatcherGetCache = cache = new IdentityHashMap<Class<?>, TypeParameterMatcher>();
//        }
//        return cache;
//    }
//
//    public Map<Class<?>, Map<String, TypeParameterMatcher>> typeParameterMatcherFindCache() {
//        Map<Class<?>, Map<String, TypeParameterMatcher>> cache = typeParameterMatcherFindCache;
//        if (cache == null) {
//            typeParameterMatcherFindCache = cache = new IdentityHashMap<Class<?>, Map<String, TypeParameterMatcher>>();
//        }
//        return cache;
//    }


    public Map<Class<?>, Boolean> handlerSharableCache() {
        Map<Class<?>, Boolean> cache = handlerSharableCache;
        if (cache == null) {
            // Start with small capacity to keep memory overhead as low as possible.
            handlerSharableCache = cache = new WeakHashMap<Class<?>, Boolean>(4);
        }
        return cache;
    }

    public int localChannelReaderStackDepth() {
        return localChannelReaderStackDepth;
    }

    public void setLocalChannelReaderStackDepth(int localChannelReaderStackDepth) {
        this.localChannelReaderStackDepth = localChannelReaderStackDepth;
    }


    /**
     * 取出数组内某个下标位置的元素
     *
     * @param index
     * @return
     */
    public Object indexedVariable(int index) {
        Object[] lookup = indexedVariables;
        return index < lookup.length ? lookup[index] : UNSET;
    }


    /**
     * 将数组内某个下标位置的数据替换为新的数据
     *
     * @param index
     * @param value
     * @return
     */
    public boolean setIndexedVariable(int index, Object value) {
        Object[] lookup = indexedVariables;
        if (index < lookup.length) {
            Object oldValue = lookup[index];
            lookup[index] = value;
            return oldValue == UNSET;
        } else {
            //数组扩容方法
            expandIndexedVariableTableAndSet(index, value);
            return true;
        }
    }


    /**
     * 数组扩容的方法，这里扩容的方法用的是某个fastthreadlocal的index。为什么要这样设置呢？ 大家可以思考一下，创建了fastthreadlocal就意味着数组的下标也就有了，换句话说，如果创建了13个threadlocal，不管这几个threadlocal
     * 是否将其对应的value存储到了数组中，但是数组要存储的数据已经确定了。如果有100多个threadlocal，那数组的下标就应该扩充到了100多
     * 当第100个threadlocal要把value存到数组中时，如果数组此时的容量为64，就要以index为基准进行扩容，因为threadlocal已经创建到了
     * 100多个，这些threadlocal对应的value迟早是要存储到本地map中的。所以，数组容量不够，就用传进来的index为基准，做位运算，得到一个
     * 2的幂次方的容量。
     *
     * @param index
     * @param value
     */
    private void expandIndexedVariableTableAndSet(int index, Object value) {
        Object[] oldArray = indexedVariables;
        final int oldCapacity = oldArray.length;
        int newCapacity = index;
        newCapacity |= newCapacity >>> 1;
        newCapacity |= newCapacity >>> 2;
        newCapacity |= newCapacity >>> 4;
        newCapacity |= newCapacity >>> 8;
        newCapacity |= newCapacity >>> 16;
        newCapacity++;
        //扩容数组，把旧的数据拷贝新数组中
        Object[] newArray = Arrays.copyOf(oldArray, newCapacity);
        //新数组扩容的那部分用UNSET赋值
        Arrays.fill(newArray, oldCapacity, newArray.length, UNSET);
        //新数组的index下标的位置赋值为value
        newArray[index] = value;
        //旧数组替换成新数组
        indexedVariables = newArray;
    }

    /**
     * 删除数组某个位置的元素，并且重新赋值为UNSET
     * @param index
     * @return
     */
    public Object removeIndexedVariable(int index) {
        Object[] lookup = indexedVariables;
        if (index < lookup.length) {
            Object v = lookup[index];
            lookup[index] = UNSET;
            return v;
        } else {
            return UNSET;
        }
    }

    public boolean isIndexedVariableSet(int index) {
        Object[] lookup = indexedVariables;
        return index < lookup.length && lookup[index] != UNSET;
    }

    public boolean isCleanerFlagSet(int index) {
        return cleanerFlags != null && cleanerFlags.get(index);
    }

    public void setCleanerFlag(int index) {
        if (cleanerFlags == null) {
            cleanerFlags = new BitSet();
        }
        cleanerFlags.set(index);
    }
}
