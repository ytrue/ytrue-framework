package com.ytrue.netty.channel;

import java.util.ArrayList;
import java.util.List;

import static com.ytrue.netty.util.internal.ObjectUtil.checkPositive;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author ytrue
 * @date 2023-08-10 9:52
 * @description AdaptiveRecvByteBufAllocator
 */
public class AdaptiveRecvByteBufAllocator extends DefaultMaxMessagesRecvByteBufAllocator {

    //下面三个属性分别表示ByteBuf的的最小容量，初始化容量，也就是第一次要分配的容量，还有就是扩容之后的最大容量，扩容不会超过这个最大容量
    static final int DEFAULT_MINIMUM = 64;
    static final int DEFAULT_INITIAL = 1024;
    static final int DEFAULT_MAXIMUM = 65536;


    //这个属性就是扩容的步长
    //其实就是得到现在ByteBuf在数组中的下标索引，加上这个步长，就是扩容之后索引下标，也就可以得到扩容之后的容量了
    //下面的缩容步长同理
    private static final int INDEX_INCREMENT = 4;

    //这个属性就是缩容的步长
    private static final int INDEX_DECREMENT = 1;

    //这个数组就是扩缩容的容量表，用数组记录，每一个下标都对应着一个容量大小
    private static final int[] SIZE_TABLE;

    static {
        //初始化一个集合
        List<Integer> sizeTable = new ArrayList<Integer>();
        //这里要像集合中添加数据，注意，当i小于512时，也就意味着容量还小于512字节，容量是按照16来递增的
        for (int i = 16; i < 512; i += 16) {
            sizeTable.add(i);
        }

        //大于512了，容量就会按照1倍来递增，一直左移最终会成为负数，所以要用i>0来做终止循环的条件
        for (int i = 512; i > 0; i <<= 1) {
            sizeTable.add(i);
        }

        //用集合的数据来初始化这个容量表数组
        SIZE_TABLE = new int[sizeTable.size()];
        for (int i = 0; i < SIZE_TABLE.length; i ++) {
            SIZE_TABLE[i] = sizeTable.get(i);
        }
    }


    @Deprecated
    public static final AdaptiveRecvByteBufAllocator DEFAULT = new AdaptiveRecvByteBufAllocator();


    /**
     * @Author: ytrue
     * @Description:在扩缩容的容量表中查找到最小大于等于initial的容量
     * 这就是纯粹的逻辑运算，大家自己搞一搞吧，经过了内存池那一章，我已经厌烦了这些数学运算。。
     */
    private static int getSizeTableIndex(final int size) {
        for (int low = 0, high = SIZE_TABLE.length - 1;;) {
            if (high < low) {
                return low;
            }
            if (high == low) {
                return high;
            }
            int mid = low + high >>> 1;
            int a = SIZE_TABLE[mid];
            int b = SIZE_TABLE[mid + 1];
            if (size > b) {
                low = mid + 1;
            } else if (size < a) {
                high = mid - 1;
            } else if (size == a) {
                return mid;
            } else {
                return mid + 1;
            }
        }
    }

    private final class HandleImpl extends MaxMessageHandle {

        //最小容量在容量表数组中的索引，最小容量为64，所以在表中的索引为3，其实就是从索引0对应的16开始递增，在512之前，按照16递增
        //得到64对应的索引为3，下面这两个索引大家可以自己算算，我就不再弄了
        private final int minIndex;
        //最大容量在容量表数组中的索引
        private final int maxIndex;
        //当前容量在容量表中的索引
        private int index;
        //下一次分配的内存大小，如果是第一次分配，这个值为1024
        //之后就是经过计算的内存大小了
        private int nextReceiveBufferSize;
        //是否需要缩容
        private boolean decreaseNow;

        /**
         * @Author: ytrue
         * @Description:构造函数
         */
        HandleImpl(int minIndex, int maxIndex, int initial) {
            this.minIndex = minIndex;
            this.maxIndex = maxIndex;
            //在扩缩容的容量表中得到容量等于initial的索引
            index = getSizeTableIndex(initial);
            //把索引位置的容量大小设置成初次要分配的ByteBuf的容量大小，为1024
            nextReceiveBufferSize = SIZE_TABLE[index];
        }

        /**
         * @Author: ytrue
         * @Description:记录本次接收到的字节的大小
         */
        @Override
        public void lastBytesRead(int bytes) {
            //这里会有一个判断，如果这一次接收到的字节数就正好等于这个可写字节数，这说明这次分配的ByteBuf刚好把所有字节接收完
            //这就意味着channel中可能还有数据没有被接收完，总之，可以看出，分配的这个ByteBuf可能不太够用，所以就会进入下面的扩容逻辑
            if (bytes == attemptedBytesRead()) {
                //扩容或者缩容
                record(bytes);
            }
            super.lastBytesRead(bytes);
        }

        /**
         * @Author: ytrue
         * @Description:分配下一次要使用的内存大小，如果是第一次分配，则使用默认值1024来分配
         * 第二次分配就不会使用默认值了，而是使用经过计算的值
         */
        @Override
        public int guess() {
            return nextReceiveBufferSize;
        }


        /**
         * @Author: ytrue
         * @Description:决定是扩容还是缩容的方法
         * actualReadBytes是本次读取到的客户端的总字节数如果这个字节数直接就大于nextReceiveBufferSize这个下一次要分配的内存容量了
         * 说明现在的ByteBuf很可能分配的有点小，所以要开始扩容，根据扩容步长，得到要扩容的容量大小
         * 而如果读取到的字节数比当前容量缩容后的容量还小，这就意味着需要缩容了
         */
        private void record(int actualReadBytes) {
            //读取到的总字节数比当前容量缩容后的容量还小，考虑缩容
            if (actualReadBytes <= SIZE_TABLE[max(0, index - INDEX_DECREMENT - 1)]) {
                //这时候decreaseNow可能还初始化，所以为false，所以进入下面的分支，把decreaseNow设为true
                //等下一次读取到的字节数仍然很少，这时候就可以真正的扩容了。因为decreaseNow已经被
                //置为true了
                if (decreaseNow) {
                    index = max(index - INDEX_DECREMENT, minIndex);
                    nextReceiveBufferSize = SIZE_TABLE[index];
                    decreaseNow = false;
                } else {
                    decreaseNow = true;
                }
                //走到这里说明接收到的字节数大于了nextReceiveBufferSize，直接扩容即可
            } else if (actualReadBytes >= nextReceiveBufferSize) {
                //但是再怎么扩容，也不能比maxIndex索引对应的容量大
                index = min(index + INDEX_INCREMENT, maxIndex);
                nextReceiveBufferSize = SIZE_TABLE[index];
                decreaseNow = false;
            }
        }

        /**
         * @Author: ytrue
         * @Description:记录本次读取到的客户端消息的总的字节数
         */
        @Override
        public void readComplete() {
            //这里会判断是扩容还是缩容
            record(totalBytesRead());
        }
    }

    /**
     * @Author: ytrue
     * @Description:这三个属性会在下面的构造方法中被赋值
     */
    private final int minIndex;
    private final int maxIndex;
    private final int initial;


    /**
     * @Author: ytrue
     * @Description:AdaptiveRecvByteBufAllocator类的构造方法
     */
    public AdaptiveRecvByteBufAllocator() {
        this(DEFAULT_MINIMUM, DEFAULT_INITIAL, DEFAULT_MAXIMUM);
    }

    /**
     * @Author: ytrue
     * @Description:在该构造方法中，属性被初始化了，也就是赋值了
     */
    public AdaptiveRecvByteBufAllocator(int minimum, int initial, int maximum) {
        checkPositive(minimum, "minimum");
        if (initial < minimum) {
            throw new IllegalArgumentException("initial: " + initial);
        }
        if (maximum < initial) {
            throw new IllegalArgumentException("maximum: " + maximum);
        }

        int minIndex = getSizeTableIndex(minimum);
        if (SIZE_TABLE[minIndex] < minimum) {
            this.minIndex = minIndex + 1;
        } else {
            this.minIndex = minIndex;
        }

        int maxIndex = getSizeTableIndex(maximum);
        if (SIZE_TABLE[maxIndex] > maximum) {
            this.maxIndex = maxIndex - 1;
        } else {
            this.maxIndex = maxIndex;
        }
        this.initial = initial;
    }


    @SuppressWarnings("deprecation")
    @Override
    public Handle newHandle() {
        //initial的值为1024
        return new HandleImpl(minIndex, maxIndex, initial);
    }

    @Override
    public AdaptiveRecvByteBufAllocator respectMaybeMoreData(boolean respectMaybeMoreData) {
        super.respectMaybeMoreData(respectMaybeMoreData);
        return this;
    }
}
