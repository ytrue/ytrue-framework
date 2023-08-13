package com.ytrue.netty.channel;

import com.ytrue.netty.buffer.ByteBuf;
import com.ytrue.netty.buffer.ByteBufAllocator;
import com.ytrue.netty.util.UncheckedBooleanSupplier;

import static com.ytrue.netty.util.internal.ObjectUtil.checkPositive;

/**
 * @author ytrue
 * @date 2023-08-10 9:39
 * @description 动态内存分配器的父类，在该类中定义了一些重要的成员变量
 */
public abstract class DefaultMaxMessagesRecvByteBufAllocator implements MaxMessagesRecvByteBufAllocator {

    private volatile int maxMessagesPerRead;
    private volatile boolean respectMaybeMoreData = true;

    public DefaultMaxMessagesRecvByteBufAllocator() {
        this(1);
    }

    public DefaultMaxMessagesRecvByteBufAllocator(int maxMessagesPerRead) {
        maxMessagesPerRead(maxMessagesPerRead);
    }

    @Override
    public int maxMessagesPerRead() {
        return maxMessagesPerRead;
    }

    /**
     * @Author: ytrue
     * @Description:该方法在DefaultChannelConfig的setRecvByteBufAllocator方法内会被调用，然后maxMessagesPerRead这个属性
     * 就会被赋值为16了
     */
    @Override
    public MaxMessagesRecvByteBufAllocator maxMessagesPerRead(int maxMessagesPerRead) {
        checkPositive(maxMessagesPerRead, "maxMessagesPerRead");
        this.maxMessagesPerRead = maxMessagesPerRead;
        return this;
    }


    public DefaultMaxMessagesRecvByteBufAllocator respectMaybeMoreData(boolean respectMaybeMoreData) {
        this.respectMaybeMoreData = respectMaybeMoreData;
        return this;
    }


    public final boolean respectMaybeMoreData() {
        return respectMaybeMoreData;
    }


    public abstract class MaxMessageHandle implements RecvByteBufAllocator.ExtendedHandle {
        private ChannelConfig config;
        //这个属性就是用于控制每次接收连接或者是读取消息字节的最大次数，默认为16次
        //但是可以通过ChannelOption.MAX_MESSAGES_PER_READ参数由用户进一步配置
        private int maxMessagePerRead;

        //本次接收客户端连接或者读取消息的总次数，这个次数一旦超过16，就会停止接收客户端连接或者读取消息了
        private int totalMessages;

        //该成员变量用于读取客户端channel读取的消息，和服务端接收客户端连接无关。
        //记录的是本次读取到的客户端消息的总字节数
        private int totalBytesRead;

        //这个属性在判断是否需要扩容时会用到，该属性实际上会被ByteBuf的可以写入字节数赋值
        //然后在读取到消息后，会用实际读取到的消息字节数和这个属性的值做判断，看消息是否读取完了，或者是否需要扩容ByteBuf
        private int attemptedBytesRead;

        //本次读取到了客户端消息的字节数，注意，这个是一次读到的，并不是累加之后的总字节数
        private int lastBytesRead;

        //该属性默认为true，在判断是否需要继续读取数据时会用到
        private final boolean respectMaybeMoreData = DefaultMaxMessagesRecvByteBufAllocator.this.respectMaybeMoreData;

        //该成员变量是用来判断本次读取客户端消息之后，ByteBuf的可写入的字节数是否等于读取到的字节数
        //因为如果是相等的话，就表明这个ByteBuf已经没有多余容量可写入字节了，这就表示客户端channel中也许还有剩余的数据没有读取到
        //如果读取到的字节数小于可写入的字节数，那么就表明客户端channel中已经没有字节数可被读取了
        private final UncheckedBooleanSupplier defaultMaybeMoreSupplier = new UncheckedBooleanSupplier() {
            @Override
            public boolean get() {
                //具体的判断方法，就是判断本次读取到的数据是否等于可写入的字节数
                return attemptedBytesRead == lastBytesRead;
            }
        };

        /**
         * @Author: ytrue
         * @Description:重置接收次数和总字节的方法
         */
        @Override
        public void reset(ChannelConfig config) {
            this.config = config;
            //这里maxMessagesPerRead()方法会返回16，所以maxMessagePerRead值也被设置成16了
            maxMessagePerRead = maxMessagesPerRead();
            totalMessages = totalBytesRead = 0;
        }

        /**
         * @Author: ytrue
         * @Description:初次分配容量的方法
         */
        @Override
        public ByteBuf allocate(ByteBufAllocator alloc) {
            //在这里可以看到，实际上还是池化的内存分配器在分配内存
            return alloc.ioBuffer(guess());
        }

        /**
         * @Author: ytrue
         * @Description:累加接收到的客户端连接次数，如果是读取客户端channel的数据，则表示读取了多少次了
         */
        @Override
        public final void incMessagesRead(int amt) {
            //累加接收到的客户端连接次数或者是累加读取消息次数
            totalMessages += amt;
        }

        /**
         * @Author: ytrue
         * @Description:本次读取到的字节数，这个字节数会累加到接收到的总的字节数中
         */
        @Override
        public void lastBytesRead(int bytes) {
            lastBytesRead = bytes;
            if (bytes > 0) {
                //累加到接收到的总字节数中
                totalBytesRead += bytes;
            }
        }

        @Override
        public final int lastBytesRead() {
            return lastBytesRead;
        }

        /**
         * @Author: ytrue
         * @Description:判断是否还要继续读取数据
         */
        @Override
        public boolean continueReading() {
            return continueReading(defaultMaybeMoreSupplier);
        }

        /**
         * @Author: ytrue
         * @Description:这个方法用来判断是否还要继续读下去，也就是接收客户端连接或者是接收客户端发送过来的数据
         * 该方法只要返回true，就可以继续在循环中读取数据了，但是达到什么条件就可以返回true了呢？首先要保证
         * totalMessages < maxMessagePerRead成立，也就是接收连接或者读取消息的次数是小于16次的
         * 然后保证totalBytesRead > 0，也就是读取到的总的数据要大于0，注意，这个属性在读取客户端数据的时候是有用的
         * 但是在服务端接收客户端连接的时候是完全用不上的，但是在服务端接收客户端连接时也用这个方法作为判断了，所以这是一个bug
         * 在后面的版本中被修复了，这个大家要弄清楚
         * 接着，还要看看maybeMoreDataSupplier.get()返回的是true还是false。这个是本次读取的数据是否等于ByteBuf的可写入字节数
         * 实际上代表的就是客户端channel中是否还有数据可以被读取
         * 加上前面的respectMaybeMoreData这个属性，这个属性默认值为true，所以 (!respectMaybeMoreData || maybeMoreDataSupplier.get())
         * 的意思就成了如果客户端channel中还有数据等待被读取，是不是需要被重视，也就是要去被重视地去强迫读取？
         * respectMaybeMoreData这个单词的翻译就是"重视也许更多的数据"。。反正就是这么个意思，但这个属性默认值为true，但是取反了为false，也就是说不用认真对待
         * 所以，就不用去读取了
         * 总之，下面这个方法，三个&&条件都成立，才会返回true，有一个不成立就可以退出循环了
         */
        @Override
        public boolean continueReading(UncheckedBooleanSupplier maybeMoreDataSupplier) {
            return config.isAutoRead() &&
                   (!respectMaybeMoreData || maybeMoreDataSupplier.get()) &&
                   totalMessages < maxMessagePerRead &&
                   totalBytesRead > 0;
        }

        @Override
        public void readComplete() {
        }

        @Override
        public int attemptedBytesRead() {
            return attemptedBytesRead;
        }

        @Override
        public void attemptedBytesRead(int bytes) {
            attemptedBytesRead = bytes;
        }

        /**
         * @Author: ytrue
         * @Description:返回读取到的总的字节数
         */
        protected final int totalBytesRead() {
            return totalBytesRead < 0 ? Integer.MAX_VALUE : totalBytesRead;
        }
    }

}
