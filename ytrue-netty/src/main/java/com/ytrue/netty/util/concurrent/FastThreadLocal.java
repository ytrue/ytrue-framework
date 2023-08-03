package com.ytrue.netty.util.concurrent;

import com.ytrue.netty.util.internal.InternalThreadLocalMap;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * @author ytrue
 * @date 2023-08-03 11:06
 * @description Netty作者自己封装的threadlocal，该threadlocal与Java原生的threadlocal的不同之处其实就体现在存入threadlocalMap时
 * Netty的数组下标是创建threadlocal时就确定的，而Java原生的threadlocal则是通过hash值求数组下标
 */
@Slf4j
public class FastThreadLocal<V> {

    /**
     * 这个可不是FastThreadLocal刚创建时的索引，虽然创建索引也是通过InternalThreadLocalMap.nextVariableIndex()方法获得的
     * 但这个属性为静态变量。会最先被赋值，这个时候得到的值是0。只会被创建一次，并且不会改变，0
     */
    private static final int variablesToRemoveIndex = InternalThreadLocalMap.nextVariableIndex();


    /**
     * 该属性就是决定了fastthreadlocal在threadlocalmap数组中的下标位置
     */
    private final int index;


    /**
     * FastThreadLocal构造器，创建的那一刻，threadlocal在map中的下标就已经确定了
     */
    public FastThreadLocal() {
        index = InternalThreadLocalMap.nextVariableIndex();
    }


    /**
     * 还记得FastThreadLocalRunnable这个类吗？removeAll方法就会在该类的run方法中被调用
     */
    public static void removeAll() {
        //得到存储数据的InternalThreadLocalMap
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
        if (threadLocalMap == null) {
            return;
        }
        try {
            //这里设计的很有意思，先通过fastthreadlocal的下标索引variablesToRemoveIndex，也就是0，
            //从存储数据的InternalThreadLocalMap中得到存储的value
            //然后做了什么呢？判断value是否为空，不为空则把该value赋值给一个set集合，再把集合转换成一个fastthreadlocal数组，遍历该数组
            //然后通过fastthreadlocal删除threadlocalmap中存储的数据。
            //这里可以看到，其实该线程引用到的每一个fastthreadlocal会组成set集合，然后被放到threadlocalmap数组的0号位置
            Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);

            if (v != null && v != InternalThreadLocalMap.UNSET) {
                @SuppressWarnings("unchecked")
                Set<FastThreadLocal<?>> variablesToRemove = (Set<FastThreadLocal<?>>) v;
                FastThreadLocal<?>[] variablesToRemoveArray =
                        variablesToRemove.toArray(new FastThreadLocal[0]);
                for (FastThreadLocal<?> tlv : variablesToRemoveArray) {
                    tlv.remove(threadLocalMap);
                }
            }
        } finally {
            //这一步是为了删除InternalThreadLocalMap或者是slowThreadLocalMap
            InternalThreadLocalMap.remove();
        }
    }


    /**
     * 得到threadlocalmap数组存储的元素个数
     *
     * @return
     */
    public static int size() {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
        if (threadLocalMap == null) {
            return 0;
        } else {
            return threadLocalMap.size();
        }
    }


    /**
     * 销毁方法
     */
    public static void destroy() {
        InternalThreadLocalMap.destroy();
    }


    /**
     * 该方法是把该线程引用的fastthreadlocal组成一个set集合，然后方到threadlocalmap数组的0号位置
     *
     * @param threadLocalMap
     * @param variable
     */
    private static void addToVariablesToRemove(InternalThreadLocalMap threadLocalMap, FastThreadLocal<?> variable) {
        //首先得到threadlocalmap数组0号位置的对象
        Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
        //定义一个set集合
        Set<FastThreadLocal<?>> variablesToRemove;

        if (v == InternalThreadLocalMap.UNSET || v == null) {
            //如果threadlocalmap的0号位置存储的数据为null，那就创建一个set集合
            variablesToRemove = Collections.newSetFromMap(new IdentityHashMap<FastThreadLocal<?>, Boolean>());
            //把InternalThreadLocalMap数组的0号位置设置成set集合
            threadLocalMap.setIndexedVariable(variablesToRemoveIndex, variablesToRemove);
        } else {
            //如果数组的0号位置不为null，就说明已经有set集合了，直接获得即可
            variablesToRemove = (Set<FastThreadLocal<?>>) v;
        }
        //把fastthreadlocal添加到set集合中
        variablesToRemove.add(variable);
    }


    /**
     * 删除set集合中的某一个fastthreadlocal对象
     *
     * @param threadLocalMap
     * @param variable
     */
    private static void removeFromVariablesToRemove(InternalThreadLocalMap threadLocalMap, FastThreadLocal<?> variable) {
        //根据0下标获得set集合
        Object v = threadLocalMap.indexedVariable(variablesToRemoveIndex);
        if (v == InternalThreadLocalMap.UNSET || v == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Set<FastThreadLocal<?>> variablesToRemove = (Set<FastThreadLocal<?>>) v;
        variablesToRemove.remove(variable);
    }

    /**
     * 得到fastthreadlocal存储在map数组中的数据
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public final V get() {
        //得到存储数据的map
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
        //根据fastthreadlocal的下标索引获得存储在数组中的数据
        Object v = threadLocalMap.indexedVariable(index);
        //如果不为未设定状态就返回
        if (v != InternalThreadLocalMap.UNSET) {
            return (V) v;
        }
        //返回该数据
        return initialize(threadLocalMap);
    }


    /**
     * 存在就返回，否则返回null
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public final V getIfExists() {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
        if (threadLocalMap != null) {
            Object v = threadLocalMap.indexedVariable(index);
            if (v != InternalThreadLocalMap.UNSET) {
                return (V) v;
            }
        }
        return null;
    }


    /**
     * 得到fastthreadlocal存储在map数组中的数据，只不过这里把map当作参数纯进来了
     *
     * @param threadLocalMap
     * @return
     */
    public final V get(InternalThreadLocalMap threadLocalMap) {
        Object v = threadLocalMap.indexedVariable(index);
        if (v != InternalThreadLocalMap.UNSET) {
            return (V) v;
        }

        return initialize(threadLocalMap);
    }

    /**
     * 这是个初始化的方法，但并不是对于threadLocalMap初始化，这个方法的意思是，如果我们还没有数据存储在threadlocalmap
     * 中，这时候就可以调用这个方法，在这个方法内进一步调用initialValue方法返回一个要存储的对象，再将它存储到map中
     * 而initialValue方法就是由用户自己实现的
     *
     * @param threadLocalMap
     * @return
     */

    private V initialize(InternalThreadLocalMap threadLocalMap) {
        V v = null;
        try {
            //该方法由用户自己实现
            v = initialValue();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        //把创建好的对象存储到map中
        threadLocalMap.setIndexedVariable(index, v);
        addToVariablesToRemove(threadLocalMap, this);
        return v;
    }


    /**
     * 把要存储的value设置到threadlocalmap中
     *
     * @param value
     */
    public final void set(V value) {
        //如果该value不是未定义状态就可以直接存放
        if (value != InternalThreadLocalMap.UNSET) {
            //得到该线程私有的threadlocalmap
            InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
            //把值设置进去
            setKnownNotUnset(threadLocalMap, value);
        } else {
            remove();
        }
    }

    /**
     * 功能同上，就不再详细讲解了
     *
     * @param threadLocalMap
     * @param value
     */
    public final void set(InternalThreadLocalMap threadLocalMap, V value) {
        if (value != InternalThreadLocalMap.UNSET) {
            setKnownNotUnset(threadLocalMap, value);
        } else {
            remove(threadLocalMap);
        }
    }

    /**
     * 设置value到本地map中
     *
     * @param threadLocalMap
     * @param value
     */
    private void setKnownNotUnset(InternalThreadLocalMap threadLocalMap, V value) {
        //设置value到本地map中
        if (threadLocalMap.setIndexedVariable(index, value)) {
            //把fastthreadlocal对象放到本地map的0号位置的set中
            addToVariablesToRemove(threadLocalMap, this);
        }
    }

    public final boolean isSet() {
        return isSet(InternalThreadLocalMap.getIfSet());
    }


    public final boolean isSet(InternalThreadLocalMap threadLocalMap) {
        return threadLocalMap != null && threadLocalMap.isIndexedVariableSet(index);
    }

    public final void remove() {
        remove(InternalThreadLocalMap.getIfSet());
    }


    /**
     * 删除InternalThreadLocalMap中的数据
     *
     * @param threadLocalMap
     */
    public final void remove(InternalThreadLocalMap threadLocalMap) {
        if (threadLocalMap == null) {
            return;
        }
        //用fastthreadlocal的下标从map中得到存储的数据
        Object v = threadLocalMap.removeIndexedVariable(index);
        //从map0号位置的set中删除fastthreadlocal对象
        removeFromVariablesToRemove(threadLocalMap, this);
        if (v != InternalThreadLocalMap.UNSET) {
            try {
                //该方法可以由用户自己实现，可以对value做一些处理
                onRemoval((V) v);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }


    /**
     * 该方法就是要被用户重写的方法
     *
     * @return
     * @throws Exception
     */
    protected V initialValue() throws Exception {
        return null;
    }

    /**
     * 该方法可以由用户自行定义扩展，在删除本地map中的数据时，可以扩展一些功能
     *
     * @param value
     * @throws Exception
     */
    protected void onRemoval(@SuppressWarnings("UnusedParameters") V value) throws Exception {
    }

}
