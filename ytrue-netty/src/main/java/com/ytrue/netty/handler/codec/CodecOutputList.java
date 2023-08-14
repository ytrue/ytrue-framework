package com.ytrue.netty.handler.codec;

import com.ytrue.netty.util.concurrent.FastThreadLocal;
import com.ytrue.netty.util.internal.MathUtil;

import java.util.AbstractList;
import java.util.RandomAccess;

import static com.ytrue.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * @author ytrue
 * @date 2023-08-14 14:25
 * @description 这个类的作用很简单，就是把累加缓冲区的字节，解码后存放到这个类的对象中，然后从这个类的对象中，把消息
 * 传递给后面的handler
 */
final class CodecOutputList extends AbstractList<Object> implements RandomAccess {

    /**
     * @Author: ytrue
     * @Description:这个是CodecOutputList对象的回收器，但这个里面什么也没干，实际上还是垃圾回收来处理CodecOutputList对象 具体原因会在下面解释
     */
    private static final CodecOutputListRecycler NOOP_RECYCLER = object -> {
        // drop on the floor and let the GC handle it.
    };

    /**
     * @Author: ytrue
     * @Description:CodecOutputList对象的对象池，这里大家要注意，从对象池中得到的其实是一个容量为16的CodecOutputLists对象 CodecOutputLists对象为内部类
     */
    private static final FastThreadLocal<CodecOutputLists> CODEC_OUTPUT_LISTS_POOL =
            new FastThreadLocal<CodecOutputLists>() {
                @Override
                protected CodecOutputLists initialValue() throws Exception {
                    //这么做其实就意味着，每一次获得out对象，实际上是从这个CodecOutputLists对象中获得一个CodecOutputList对象
                    //因为CodecOutputLists容量为16，所以16个CodecOutputList对象都获得完了，没有新的对象可获得了
                    //就会创建一个不会缓存的CodecOutputList对象，而这个对象内部只能存放4个消息对象
                    //所以逻辑已经很清晰了，解码后的消息对象明确放在CodecOutputList对象内部的数组中，但是中间会经过CodecOutputLists对象
                    //从该对象中获得CodecOutputList对象，而CodecOutputLists对象是从对象池中获得的。这个逻辑要理清楚
                    return new CodecOutputLists(16);
                }
            };


    /**
     * @Author: ytrue
     * @Description:回收CodecOutputList对象的接口
     */
    private interface CodecOutputListRecycler {
        void recycle(CodecOutputList codecOutputList);
    }

    /**
     * @Author: ytrue
     * @Description:刚刚提到的内部类
     */
    private static final class CodecOutputLists implements CodecOutputListRecycler {
        //存放CodecOutputList对象的数组，CodecOutputList对象会被拿走用来存储解码后的消息对象
        private final CodecOutputList[] elements;
        //掩码，用啦计算数组下标的
        private final int mask;
        //当前的索引
        private int currentIdx;
        //CodecOutputList对象的个数
        private int count;

        /**
         * @Author: ytrue
         * @Description:构造方法
         */
        CodecOutputLists(int numElements) {
            //创建数组，这里的数组长度会设置成2的幂次方
            elements = new CodecOutputList[MathUtil.safeFindNextPositivePowerOfTwo(numElements)];
            //为数组的每个位置赋值
            for (int i = 0; i < elements.length; ++i) {
                //这里也可以看到，一个CodecOutputList对象可以存储16个解码后的消息对象
                elements[i] = new CodecOutputList(this, 16);
            }
            //CodecOutputList个数赋值
            count = elements.length;
            //当前索引设置为数组长度，这里其实能看出来，从数组中取出CodecOutputList对象，是从后往前取的
            //不然就会把当前索引置为0了
            currentIdx = elements.length;
            //掩码赋值，数组长度减1，想起hashmap中计算下标的方法了吗？都是一样一样的
            //可见这种长度设置成2次幂，然后掩码设置为长度减1，是各个变成大佬公认的一种稍微提高性能的方法
            mask = elements.length - 1;
        }

        /**
         * @Author: ytrue
         * @Description:从CodecOutputLists对象中得到一个CodecOutputList对象
         */
        public CodecOutputList getOrCreate() {
            //这里会判断CodecOutputLists的数组中剩余的CodecOutputList个数
            if (count == 0) {
                //如果个数为0，就要创建新的不会被缓存的CodecOutputList对象，该对象有垃圾回收处理，所以把一个
                //空实现的回收器传进去了，然后设置容量长度为4。正常情况下，CodecOutputList对象内部的数组长度也为16
                return new CodecOutputList(NOOP_RECYCLER, 4);
            }
            //CodecOutputList数量减1
            --count;
            //这里可以看出来是从CodecOutputLists数组中从后向前取走CodecOutputList的
            int idx = (currentIdx - 1) & mask;
            //注意哦，这里并没有把数组中相应位置的引用置为null
            CodecOutputList list = elements[idx];
            //当前索引赋值
            currentIdx = idx;
            //返回CodecOutputList对象
            return list;
        }

        /**
         * @Author: ytrue
         * @Description:该方法会释放CodecOutputList对象到数组中
         */
        @Override
        public void recycle(CodecOutputList codecOutputList) {
            //得到当前索引
            int idx = currentIdx;
            //直接放回去即可
            elements[idx] = codecOutputList;
            //重置当前索引
            currentIdx = (idx + 1) & mask;
            ++count;
            assert count <= elements.length;
        }
    }

    /**
     * @Author: ytrue
     * @Description:从对象池中获得CodecOutputList对象的方法
     */
    static CodecOutputList newInstance() {
        return CODEC_OUTPUT_LISTS_POOL.get().getOrCreate();
    }

    //对象池回收句柄
    private final CodecOutputListRecycler recycler;
    //存放的消息对象的个数
    private int size;
    //存放消息对象的数组
    private Object[] array;
    //是否有消息对象存放进来过，有消息对象放进来后，该属性就会被置为true了，只有在CodecOutputList被回收的时候才被置为false
    private boolean insertSinceRecycled;

    private CodecOutputList(CodecOutputListRecycler recycler, int size) {
        this.recycler = recycler;
        array = new Object[size];
    }

    @Override
    public Object get(int index) {
        checkIndex(index);
        return array[index];
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * @Author: ytrue
     * @Description:添加消息对象到CodecOutputList对象的数组中
     */
    @Override
    public boolean add(Object element) {
        checkNotNull(element, "element");
        try {
            insert(size, element);
        } catch (IndexOutOfBoundsException ignore) {
            expandArray();
            insert(size, element);
        }
        //消息对象个数加1
        ++size;
        return true;
    }

    @Override
    public Object set(int index, Object element) {
        checkNotNull(element, "element");
        checkIndex(index);

        Object old = array[index];
        insert(index, element);
        return old;
    }

    /**
     * @Author: ytrue
     * @Description:添加到指定的位置
     */
    @Override
    public void add(int index, Object element) {
        checkNotNull(element, "element");
        checkIndex(index);
        if (size == array.length) {
            //是否需要扩容
            expandArray();
        }
        if (index != size) {
            System.arraycopy(array, index, array, index + 1, size - index);
        }
        insert(index, element);
        ++size;
    }

    /**
     * @Author: ytrue
     * @Description:删除固定位置的消息对象
     */
    @Override
    public Object remove(int index) {
        checkIndex(index);
        Object old = array[index];
        int len = size - index - 1;
        if (len > 0) {
            System.arraycopy(array, index + 1, array, index, len);
        }
        array[--size] = null;
        return old;
    }

    /**
     * @Author: ytrue
     * @Description:清空消息对象个数，这里只是把size置为0了，并不是真的清空了
     */
    @Override
    public void clear() {
        size = 0;
    }


    boolean insertSinceRecycled() {
        return insertSinceRecycled;
    }

    /**
     * @Author: ytrue
     * @Description:释放CodecOutputList对象的外层方法
     */
    void recycle() {
        for (int i = 0; i < size; i++) {
            array[i] = null;
        }
        size = 0;
        insertSinceRecycled = false;
        //真正回收的方法
        recycler.recycle(this);
    }


    Object getUnsafe(int index) {
        return array[index];
    }

    private void checkIndex(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * @Author: ytrue
     * @Description:将消息对象添加到固定位置
     */
    private void insert(int index, Object element) {
        array[index] = element;
        insertSinceRecycled = true;
    }


    /**
     * @Author: ytrue
     * @Description:扩容的方法
     */
    private void expandArray() {
        //左移一位，扩容2倍
        int newCapacity = array.length << 1;
        if (newCapacity < 0) {
            throw new OutOfMemoryError();
        }
        Object[] newArray = new Object[newCapacity];
        System.arraycopy(array, 0, newArray, 0, array.length);
        array = newArray;
    }
}
