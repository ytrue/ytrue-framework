package com.ytrue.netty.channel;

import com.ytrue.netty.buffer.ByteBuf;
import com.ytrue.netty.util.Recycler;
import com.ytrue.netty.util.ReferenceCountUtil;
import com.ytrue.netty.util.concurrent.FastThreadLocal;
import com.ytrue.netty.util.internal.InternalThreadLocalMap;
import com.ytrue.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import static java.lang.Math.min;



/**
 * @Author: ytrue
 * @Description:写缓冲区，要发送出去的数据首先会被放到该缓冲区中，然后再从该队列中发送到socket中
 * 它的内部是一个Entry链表，并且是单向链表，而且采用的是尾插法
 * 这个写缓冲区和socket缓冲区不是一回事，大家要注意区分
 */
public final class ChannelOutboundBuffer {
    //这个就是一个Entry对象的大小，默认为96字节
    static final int CHANNEL_OUTBOUND_BUFFER_ENTRY_OVERHEAD =
            SystemPropertyUtil.getInt("io.netty.transport.outboundBufferEntrySizeOverhead", 96);

    private static final Logger logger = LoggerFactory.getLogger(ChannelOutboundBuffer.class);

    //一个ByteBuffer[]的对象池
    private static final FastThreadLocal<ByteBuffer[]> NIO_BUFFERS = new FastThreadLocal<ByteBuffer[]>() {
        @Override
        protected ByteBuffer[] initialValue() throws Exception {
            //默认的初始容量为1024
            return new ByteBuffer[1024];
        }
    };

    //表明属于那个channel
    private final Channel channel;
    //这个就是写缓冲区中要刷新到socket中的第一个entry
    private Entry flushedEntry;
    //还未发送的第一个entry，其实就是entry链表的头节点
    //发送数据的时候，flushedEntry会指向unflushedEntry
    private Entry unflushedEntry;
    //entry链表的尾节点，也就是最后一个要发送的entry
    private Entry tailEntry;
    //累计要刷新多少个entry的个数
    private int flushed;
    //要发送的ByteBuffer的数量
    private int nioBufferCount;
    //要发送的总的ByteBuffer的字节大小
    private long nioBufferSize;
    //是否刷新失败了
    private boolean inFail;

    //totalPendingSize属性的原子更新器
    private static final AtomicLongFieldUpdater<ChannelOutboundBuffer> TOTAL_PENDING_SIZE_UPDATER =
            AtomicLongFieldUpdater.newUpdater(ChannelOutboundBuffer.class, "totalPendingSize");

    //这个属性记录的就是发送的数据大小，这个属性会和高低水位线做对比，以此决定channel的socket是否还可写
    @SuppressWarnings("UnusedDeclaration")
    private volatile long totalPendingSize;

    //unwritable属性的原子更新器
    private static final AtomicIntegerFieldUpdater<ChannelOutboundBuffer> UNWRITABLE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(ChannelOutboundBuffer.class, "unwritable");

    //这个属性是表示socket是否可写，0为可写，1为不可写
    @SuppressWarnings("UnusedDeclaration")
    private volatile int unwritable;

    private volatile Runnable fireChannelWritabilityChangedTask;
    //构造器方法
    ChannelOutboundBuffer(AbstractChannel channel) {
        this.channel = channel;
    }

    /**
     * @Author: ytrue
     * @Description:这个方法就是把消息放到写缓冲区的entry链表中，采用的是尾插法
     */
    public void addMessage(Object msg, int size, ChannelPromise promise) {
        //从entry的对象池中获取一个entry对象
        Entry entry = Entry.newInstance(msg, size, total(msg), promise);

        //这里要提醒大家一下tailEntry，flushedEntry和unflushedEntry在初始化的时候都是null
        //所以这里会判断一下是不是null
        if (tailEntry == null) {
            flushedEntry = null;
        } else {
            //走到这里说明不是null，也就意味着已经插入过带发送的消息了
            //之所以得到尾节点，因为采用的是尾插法
            Entry tail = tailEntry;
            tail.next = entry;
        }
        //走到这里说明是null，说明是第一次添加消息，直接给了尾节点就行
        tailEntry = entry;
        if (unflushedEntry == null) {
            unflushedEntry = entry;
        }
        //更新写缓冲区存入的字节总量
        incrementPendingOutboundBytes(entry.pendingSize, false);
    }

    /**
     * @Author: ytrue
     * @Description:准备刷新数据到socket中
     */
    public void addFlush() {
        //这里得到的就是第一个没有发送的消息对象，其实得到的就是没有entry链表的头节点
        Entry entry = unflushedEntry;
        if (entry != null) {
            if (flushedEntry == null) {
                //在这里把要发送的第一个消息赋值成功
                flushedEntry = entry;
            }
            do {
                //把计数加1
                flushed ++;
                //设置promise为不可取消状态
                //这里判断了一下entry对应的promise是否成功设置为不可取消状态了
                //注意，这里为什么会判断entry中的promise，其实这里判断的还是对应的channel，因为消息的发送是由socketChannel发起的
                //这里其实是判断发送消息的这个动作对应的promise是否成功设置成不可取消的了
                //如果消息开始发送，发送消息的这个动作就不能被取消了，这一点要理清楚
                //不可能程序正在把消息往socket中刷新，结果发送消息突然取消了
                if (!entry.promise.setUncancellable()) {
                    //如果设置不可取消失败，说明这个发送消息的操作已经被用户取消了
                    //释放entry持有的那个msg，这个msg实际上就是使用了直接内存的ByteBuf
                    //这里返回的就是释放了的字节大小
                    int pending = entry.cancel();
                    //既然释放了这么多的字节，就要原子更新totalPendingSize的值，这里是减去释放了的字节数
                    decrementPendingOutboundBytes(pending, false, true);
                }
                //得到下一个entry，判断它的状态是否设置完善，也就是是否设置为不可取消的了
                entry = entry.next;
            } while (entry != null);
            //unflushedEntry置为null
            unflushedEntry = null;
        }
    }


    void incrementPendingOutboundBytes(long size) {
        incrementPendingOutboundBytes(size, true);
    }

    /**
     * @Author: ytrue
     * @Description:更新写缓冲区增加的字节总量
     */
    private void incrementPendingOutboundBytes(long size, boolean invokeLater) {
        //如果size为0，直接返回
        if (size == 0) {
            return;
        }
        //用TOTAL_PENDING_SIZE_UPDATER原子更新器把新存入的字节加上去
        //addAndGet相当于++i
        long newWriteBufferSize = TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, size);
        //这里就比较关键了，其实就是判断totalPendingSize是不是大于了高水位线
        if (newWriteBufferSize > channel.config().getWriteBufferHighWaterMark()) {
            //如果高于了设置当前的socketChannel为不可写状态
            setUnwritable(invokeLater);
        }
    }


    void decrementPendingOutboundBytes(long size) {
        decrementPendingOutboundBytes(size, true, true);
    }


    /**
     * @Author: ytrue
     * @Description:更新写缓冲区增加的字节总量，这里是减少
     * 原理和之前那个增加总量类似，只不过这里面是设置channel可写而已
     */
    private void decrementPendingOutboundBytes(long size, boolean invokeLater, boolean notifyWritability) {
        if (size == 0) {
            return;
        }
        long newWriteBufferSize = TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, -size);
        if (notifyWritability && newWriteBufferSize < channel.config().getWriteBufferLowWaterMark()) {
            setWritable(invokeLater);
        }
    }

    private static long total(Object msg) {
        if (msg instanceof ByteBuf) {
            return ((ByteBuf) msg).readableBytes();
        }
        if (msg instanceof FileRegion) {
            return ((FileRegion) msg).count();
        }
//        if (msg instanceof ByteBufHolder) {
//            return ((ByteBufHolder) msg).content().readableBytes();
//        }
        return -1;
    }


    public Object current() {
        Entry entry = flushedEntry;
        if (entry == null) {
            return null;
        }
        return entry.msg;
    }


    public long currentProgress() {
        Entry entry = flushedEntry;
        if (entry == null) {
            return 0;
        }
        return entry.progress;
    }


    /**
     * @Author: ytrue
     * @Description:更新一下entry的发送进度
     */
    public void progress(long amount) {
        Entry e = flushedEntry;
        assert e != null;
        ChannelPromise p = e.promise;
        long progress = e.progress + amount;
        e.progress = progress;
//        if (p instanceof ChannelProgressivePromise) {
//            ((ChannelProgressivePromise) p).tryProgress(progress, e.total);
//        }
    }


    /**
     * @Author: ytrue
     * @Description:删除写缓冲区中的链表的首节点的方法，其实就是删除当前刷新的这个entry节点
     */
    public boolean remove() {
        Entry e = flushedEntry;
        if (e == null) {
            //如果e为null，说明没有要发送的节点
            //清除一下数组
            clearNioBuffers();
            return false;
        }
        Object msg = e.msg;
        ChannelPromise promise = e.promise;
        int size = e.pendingSize;
        //删除flushedEntry对应的entry
        removeEntry(e);
        //这里会先判断一下entry对应的发送数据的操作是否被取消了
        //正常情况应该是没被取消，也就是返回false
        //然后取反，才能继续执行下去
        if (!e.cancelled) {
            //释放msg内存，其实释放的就是包装msg直接内存
            ReferenceCountUtil.safeRelease(msg);
            safeSuccess(promise);
            decrementPendingOutboundBytes(size, false, true);
        }
        //回收entry对象到对象池
        e.recycle();
        return true;
    }


    public boolean remove(Throwable cause) {
        return remove0(cause, true);
    }

    /**
     * @Author: ytrue
     * @Description:删除写缓冲区中的链表的首节点的方法
     */
    private boolean remove0(Throwable cause, boolean notifyWritability) {
        Entry e = flushedEntry;
        if (e == null) {
            //清除一下数组，这里清空数组是考虑到了该remove0方法被调用的情况
            //如果是在本类中的failFlushed方法中调用了remove0方法
            //那发送消息的channel都有可能断开连接了，所以这里清空一下所有要发送的数据也是可以的
            clearNioBuffers();
            return false;
        }
        Object msg = e.msg;
        ChannelPromise promise = e.promise;
        //得到entry对象占用的总的内存大小
        int size = e.pendingSize;
        //从链表中删除entry节点
        removeEntry(e);
        if (!e.cancelled) {
            ReferenceCountUtil.safeRelease(msg);
            safeFail(promise, cause);
            decrementPendingOutboundBytes(size, false, notifyWritability);
        }
        e.recycle();
        return true;
    }

    /**
     * @Author: ytrue
     * @Description:删除entry节点的方法
     */
    private void removeEntry(Entry e) {
        //判断计数是否为0，为0则意味着没有要刷新的节点了
        if (-- flushed == 0) {
            flushedEntry = null;
            if (e == tailEntry) {
                tailEntry = null;
                unflushedEntry = null;
            }
        } else {
            //更新当前flushedEntry为下一个节点
            flushedEntry = e.next;
        }
    }

    /**
     * @Author: ytrue
     * @Description:这个方法其实很有意思，因为说到底刷新数据到socket缓冲区中，还是按照字节刷新的，并不是按照entry刷新的
     * 这就有可能导致正在刷新一个entry对象中的数据，但是还没完全刷新完，socket就不可写了
     * 那这个entry对象要怎么办呢？数据没有刷新完，肯定不能就直接从写缓冲区的链表中删除吧。
     * 这种情况就会更新flushedEntry对应的entry中的包装msg的ByteBuf的readerIndex指针，为下一次刷新数据作准备
     */
    public void removeBytes(long writtenBytes) {
        for (;;) {
            //得到当前发送的entry节点内部的msg
            Object msg = current();
            if (!(msg instanceof ByteBuf)) {
                assert writtenBytes == 0;
                break;
            }
            final ByteBuf buf = (ByteBuf) msg;
            final int readerIndex = buf.readerIndex();
            //在这里计算出ByteBuf中的可读字节数，也就是可以发送的字节数
            final int readableBytes = buf.writerIndex() - readerIndex;
            //这里就是具体对比的方法了，注意哦，整个方法是在一个for循环中执行的，会循环判断这些已经发送了的
            //ByteBuf的总的字节大小是否和刚才刷新成功的字节大小是否相等
            if (readableBytes <= writtenBytes) {
                //走到这里说明还不想等，并且BytBuf的可以发送的字节数小于刷新成功的总数
                //这说明现在的这个entry已经完全刷新了，所以可以删去了
                if (writtenBytes != 0) {
                    progress(readableBytes);
                    //删去entry节点之前，先把刷新成功的总字节数更新一下，判断了一个entry，就要把判断的那些字节都减去
                    writtenBytes -= readableBytes;
                }
                //因为整个entry中的数据已经发送完了，所以删除这个entry就行了
                //在这个删除entry的方法中，flushedEntry会被更新为下一个节点，方便下一次判断
                //然后就进入下一轮循环了
                remove();
            } else { // readableBytes > writtenBytes
                //如果减到最后，发现当前ByteBuf的readableBytes终于大于writtenBytes了
                //这就意味着终于找到这个刷新不完整的ByteBuf了
                //而且这时候writtenBytes也一直在更新
                if (writtenBytes != 0) {
                    //这里就会更新一下ByteBuf中的读指针，为下一次刷新数据作准备
                    //就是把读指针从0移动到writtenBytes这个位置
                    //因为这时候writtenBytes的值就是当前ByteBuf中的可读字节
                    //大家可以举个例子代入一下，比如一共有三个ByteBuf，每个存放两个字节，一共有6个字节，但是刷新了5个字节
                    //按照这个循环中的逻辑计算一下，就会恍然大悟了
                    buf.readerIndex(readerIndex + (int) writtenBytes);
                    progress(writtenBytes);
                }
                break;
            }
        }
        //清除一下nioBuffers数组，因为这些ByteBuffer都已经成功刷新了
        clearNioBuffers();
    }


    /**
     * @Author: ytrue
     * @Description:清空nioBuffers数组
     */
    private void clearNioBuffers() {
        int count = nioBufferCount;
        if (count > 0) {
            nioBufferCount = 0;
            Arrays.fill(NIO_BUFFERS.get(), 0, count, null);
        }
    }


    public ByteBuffer[] nioBuffers() {
        return nioBuffers(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }


    /**
     * @Author: ytrue
     * @Description:这个方法就是把写缓冲区中的ByteBuf转换成ByteBuffer数组
     */
    public ByteBuffer[] nioBuffers(int maxCount, long maxBytes) {
        assert maxCount > 0;
        assert maxBytes > 0;

        long nioBufferSize = 0;
        int nioBufferCount = 0;

        final InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();

        //在这里用到了上面那个对象池，从对象池中获取nioBuffers数组
        ByteBuffer[] nioBuffers = NIO_BUFFERS.get(threadLocalMap);

        //获取要发送的第一个entry节点
        Entry entry = flushedEntry;
        //下面就是一个循环，在循环中依次得到entry节点
        while (isFlushedEntry(entry) && entry.msg instanceof ByteBuf) {
            //判断发送消息是否被取消了，没取消才可以继续向下执行
            if (!entry.cancelled) {
                //得到真正包装msg的ByteBuf
                ByteBuf buf = (ByteBuf) entry.msg;
                //得到读指针
                final int readerIndex = buf.readerIndex();
                //写指针减去读指针，得到要发送的消息的字节大小
                final int readableBytes = buf.writerIndex() - readerIndex;
                //判断可发送的数据是否大于0的
                if (readableBytes > 0) {
                    //如果大于0，就用本次可以发送的最大字节数减去这个ByteBuf中可发送的数据的字节大小
                    if (maxBytes - readableBytes < nioBufferSize && nioBufferCount != 0) {
                        //直到循环一定次数之后，判断了多个ByteBuf之后，本次可发送的最大字节数要超过maxBytes
                        //这时候就可以退出循环了，因为本次发送的最大字节数不能超过maxBytes
                        break;
                    }

                    //这里累加一下，得到总的要发送的字节数
                    nioBufferSize += readableBytes;

                    //得到该entry中有几个ByteBuffer
                    int count = entry.count;
                    if (count == -1) {
                        //这里count会被赋值为1，因为buf.nioBufferCount()返回值为1
                        entry.count = count = buf.nioBufferCount();
                    }

                    //这里取最小值
                    int neededSpace = min(maxCount, nioBufferCount + count);
                    //这里会判断本次可以发送的ByteBuffer的个数是否大于数组现有的容量
                    if (neededSpace > nioBuffers.length) {
                        //大于就扩容nioBuffers数组
                        nioBuffers = expandNioBufferArray(nioBuffers, neededSpace, nioBufferCount);
                        NIO_BUFFERS.set(threadLocalMap, nioBuffers);
                    }

                    //这里count已经被赋值为1了
                    if (count == 1) {
                        ByteBuffer nioBuf = entry.buf;
                        if (nioBuf == null) {
                            //通过buf.internalNioBuffer(readerIndex, readableBytes)把
                            //ByteBuf先转换成它内部的ByteBuffer
                            //然后再把这个ByteBuffer赋给entry中的ByteBuffer
                            entry.buf = nioBuf = buf.internalNioBuffer(readerIndex, readableBytes);
                        }
                        //这里把ByteBuffer放到数组中
                        nioBuffers[nioBufferCount++] = nioBuf;
                    } else {
                        //如果不是1，说明有多个，就循环放到数组中
                        nioBufferCount = nioBuffers(entry, buf, nioBuffers, nioBufferCount, maxCount);
                    }
                    if (nioBufferCount == maxCount) {
                        break;
                    }
                }
            }
            entry = entry.next;
        }
        //这里给这两个成员变量赋值，这时候已经跳出循环了
        this.nioBufferCount = nioBufferCount;
        this.nioBufferSize = nioBufferSize;

        return nioBuffers;
    }

    private static int nioBuffers(Entry entry, ByteBuf buf, ByteBuffer[] nioBuffers, int nioBufferCount, int maxCount) {
        ByteBuffer[] nioBufs = entry.bufs;
        if (nioBufs == null) {
            entry.bufs = nioBufs = buf.nioBuffers();
        }
        for (int i = 0; i < nioBufs.length && nioBufferCount < maxCount; ++i) {
            ByteBuffer nioBuf = nioBufs[i];
            if (nioBuf == null) {
                break;
            } else if (!nioBuf.hasRemaining()) {
                continue;
            }
            nioBuffers[nioBufferCount++] = nioBuf;
        }
        return nioBufferCount;
    }

    /**
     * @Author: ytrue
     * @Description:在这个方法中扩容了ByteBuffer[]
     */
    private static ByteBuffer[] expandNioBufferArray(ByteBuffer[] array, int neededSpace, int size) {
        int newCapacity = array.length;
        do {
            newCapacity <<= 1;
            if (newCapacity < 0) {
                throw new IllegalStateException();
            }
            //neededSpace的值有可能为1024，因为1024是可以转换的ByteBuffer的最大块数。
            //而1024的二进制为  10000000000
            //所以这里数组的长度其实会等于1024的时候把循环打破
        } while (neededSpace > newCapacity);

        ByteBuffer[] newArray = new ByteBuffer[newCapacity];
        System.arraycopy(array, 0, newArray, 0, size);

        return newArray;
    }


    public int nioBufferCount() {
        return nioBufferCount;
    }


    public long nioBufferSize() {
        return nioBufferSize;
    }


    /**
     * @Author: ytrue
     * @Description:判断channel是否可写
     */
    public boolean isWritable() {
        return unwritable == 0;
    }


    public boolean getUserDefinedWritability(int index) {
        return (unwritable & writabilityMask(index)) == 0;
    }


    public void setUserDefinedWritability(int index, boolean writable) {
        if (writable) {
            setUserDefinedWritability(index);
        } else {
            clearUserDefinedWritability(index);
        }
    }

    private void setUserDefinedWritability(int index) {
        final int mask = ~writabilityMask(index);
        for (;;) {
            final int oldValue = unwritable;
            final int newValue = oldValue & mask;
            if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
                if (oldValue != 0 && newValue == 0) {
                    fireChannelWritabilityChanged(true);
                }
                break;
            }
        }
    }

    private void clearUserDefinedWritability(int index) {
        final int mask = writabilityMask(index);
        for (;;) {
            final int oldValue = unwritable;
            final int newValue = oldValue | mask;
            if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
                if (oldValue == 0 && newValue != 0) {
                    fireChannelWritabilityChanged(true);
                }
                break;
            }
        }
    }

    private static int writabilityMask(int index) {
        if (index < 1 || index > 31) {
            throw new IllegalArgumentException("index: " + index + " (expected: 1~31)");
        }
        return 1 << index;
    }

    /**
     * @Author: ytrue
     * @Description:设置channel可写的方法，0为可写，1为不可写
     */
    private void setWritable(boolean invokeLater) {
        for (;;) {
            final int oldValue = unwritable;
            final int newValue = oldValue & ~1;
            //原子更新器更新
            if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
                if (oldValue != 0 && newValue == 0) {
                    //在这里还触发了一个回调事件，就是channel不可写之后回调handler中的方法
                    //当然，前提是用户的handler中重写了channelWritabilityChanged方法
                    fireChannelWritabilityChanged(invokeLater);
                }
                break;
            }
        }
    }

    /**
     * @Author: ytrue
     * @Description:设置channel为不可写入的状态，0为可写，1为不可写
     */
    private void setUnwritable(boolean invokeLater) {
        for (;;) {
            final int oldValue = unwritable;
            final int newValue = oldValue | 1;
            //原子更新器更新
            if (UNWRITABLE_UPDATER.compareAndSet(this, oldValue, newValue)) {
                if (oldValue == 0 && newValue != 0) {
                    //在这里还触发了一个回调事件，就是channel不可写之后回调handler中的方法
                    //当然，前提是用户的handler中重写了channelWritabilityChanged方法
                    fireChannelWritabilityChanged(invokeLater);
                }
                break;
            }
        }
    }

    /**
     * @Author: ytrue
     * @Description:该方法就是用来在pipeline中触发fireChannelWritabilityChanged事件，在用户自定义的handler中
     * 重写channelWritabilityChanged方法，就可以做出不同的判断处理
     */
    private void fireChannelWritabilityChanged(boolean invokeLater) {
        final ChannelPipeline pipeline = channel.pipeline();
        if (invokeLater) {
            //封装一个异步任务，这下本类中的fireChannelWritabilityChangedTask成员变量终于用上了
            Runnable task = fireChannelWritabilityChangedTask;
            if (task == null) {
                fireChannelWritabilityChangedTask = task = new Runnable() {
                    @Override
                    public void run() {
                        //触发回调
                        pipeline.fireChannelWritabilityChanged();
                    }
                };
            }
            //异步任务提交给单线程执行器
            channel.eventLoop().execute(task);
        } else {
            pipeline.fireChannelWritabilityChanged();
        }
    }


    public int size() {
        return flushed;
    }


    public boolean isEmpty() {
        return flushed == 0;
    }


    void failFlushed(Throwable cause, boolean notify) {
        if (inFail) {
            return;
        }
        try {
            inFail = true;
            for (;;) {
                if (!remove0(cause, notify)) {
                    break;
                }
            }
        } finally {
            inFail = false;
        }
    }

    void close(final Throwable cause, final boolean allowChannelOpen) {
        if (inFail) {
            channel.eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    close(cause, allowChannelOpen);
                }
            });
            return;
        }

        inFail = true;

        if (!allowChannelOpen && channel.isOpen()) {
            throw new IllegalStateException("close() must be invoked after the channel is closed.");
        }

        if (!isEmpty()) {
            throw new IllegalStateException("close() must be invoked after all flushed writes are handled.");
        }

        try {
            Entry e = unflushedEntry;
            while (e != null) {
                int size = e.pendingSize;
                TOTAL_PENDING_SIZE_UPDATER.addAndGet(this, -size);

                if (!e.cancelled) {
                    ReferenceCountUtil.safeRelease(e.msg);
                    safeFail(e.promise, cause);
                }
                e = e.recycleAndGetNext();
            }
        } finally {
            inFail = false;
        }
        clearNioBuffers();
    }

    void close(ClosedChannelException cause) {
        close(cause, false);
    }

    private static void safeSuccess(ChannelPromise promise) {
        //暂歇注释掉下面这行代码
        //PromiseNotificationUtil.trySuccess(promise, null, promise instanceof VoidChannelPromise ? null : logger);
    }

    private static void safeFail(ChannelPromise promise, Throwable cause) {
        //暂歇注释掉下面这行代码
        //PromiseNotificationUtil.tryFailure(promise, cause, promise instanceof VoidChannelPromise ? null : logger);
    }

    @Deprecated
    public void recycle() {
        // NOOP
    }

    public long totalPendingWriteBytes() {
        return totalPendingSize;
    }


    public long bytesBeforeUnwritable() {
        long bytes = channel.config().getWriteBufferHighWaterMark() - totalPendingSize;
        if (bytes > 0) {
            return isWritable() ? bytes : 0;
        }
        return 0;
    }


    public long bytesBeforeWritable() {
        long bytes = totalPendingSize - channel.config().getWriteBufferLowWaterMark();
        if (bytes > 0) {
            return isWritable() ? 0 : bytes;
        }
        return 0;
    }


    public void forEachFlushedMessage(MessageProcessor processor) throws Exception {
        if (processor == null) {
            throw new NullPointerException("processor");
        }

        Entry entry = flushedEntry;
        if (entry == null) {
            return;
        }

        do {
            if (!entry.cancelled) {
                if (!processor.processMessage(entry.msg)) {
                    return;
                }
            }
            entry = entry.next;
        } while (isFlushedEntry(entry));
    }

    private boolean isFlushedEntry(Entry e) {
        return e != null && e != unflushedEntry;
    }

    public interface MessageProcessor {

        boolean processMessage(Object msg) throws Exception;
    }


    /**
     * @Author: ytrue
     * @Description:又是一个Entry内部类，这个内部类和内存池中的内部类并不一样，这个要注意区分
     */
    static final class Entry {
        //Entry的对象池
        private static final Recycler<Entry> RECYCLER = new Recycler<Entry>() {
            @Override
            protected Entry newObject(Handle<Entry> handle) {
                return new Entry(handle);
            }
        };

        //对象池的外部句柄，回收对象时会用到
        private final Recycler.Handle<Entry> handle;
        //entry是一个链表，而且是单向链表，这里自然要得到下一个节点的指针
        Entry next;
        //待刷新的数据就会放到这里
        Object msg;
        //这个数组引用会在nioBuffers方法中用到
        ByteBuffer[] bufs;
        //ByteBuf转换成ByteBuffer后会将引用也交给Entry对象
        ByteBuffer buf;
        ChannelPromise promise;
        //这个是当前Entry刷新了多少数据的一个进度
        long progress;
        //这个属性表示该Entry对象要刷新的总的数据
        //并不包含Entry对象的大小哦
        long total;
        //这个是Entry对象包含的总的内存大小
        //在构造函数中可以看到，这个属性为要刷新的数据的大小加上Entry对象的96字节的大小
        int pendingSize;
        //该属性表示要刷新的数据被几个ByteBuffer包装着
        //这个值初始化为-1，后面会被赋值为1
        //entry对象回收的时候会重置为-1
        int count = -1;
        //发送消息的操作是否被取消
        boolean cancelled;

        private Entry(Recycler.Handle<Entry> handle) {
            this.handle = handle;
        }

        static Entry newInstance(Object msg, int size, long total, ChannelPromise promise) {
            Entry entry = RECYCLER.get();
            entry.msg = msg;
            //pendingSize属性为两个值的和
            entry.pendingSize = size + CHANNEL_OUTBOUND_BUFFER_ENTRY_OVERHEAD;
            entry.total = total;
            entry.promise = promise;
            return entry;
        }

        //释放包装msg的直接内存
        int cancel() {
            if (!cancelled) {
                cancelled = true;
                int pSize = pendingSize;
                ReferenceCountUtil.safeRelease(msg);
                //这里实际上会给msg赋一个空的Buffer，但是我们没有引入Unpooled，所以就注释掉了
                //msg = Unpooled.EMPTY_BUFFER;
                pendingSize = 0;
                total = 0;
                progress = 0;
                bufs = null;
                buf = null;
                return pSize;
            }
            return 0;
        }

        //回收entry对象
        void recycle() {
            next = null;
            bufs = null;
            buf = null;
            msg = null;
            promise = null;
            progress = 0;
            total = 0;
            pendingSize = 0;
            count = -1;
            cancelled = false;
            handle.recycle(this);
        }

        //回收当前entry对象并返回链表下一个节点对象
        Entry recycleAndGetNext() {
            Entry next = this.next;
            recycle();
            return next;
        }
    }
}

