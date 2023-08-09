package com.ytrue.netty.buffer;

import com.ytrue.netty.util.internal.StringUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author ytrue
 * @date 2023-08-08 9:16
 * @description 该类的对象会组成一个链表，每一个ChunkList链表都拥有自己的内存利用率
 */
final class PoolChunkList<T> implements PoolChunkListMetric {

    private static final Iterator<PoolChunkMetric> EMPTY_METRICS = Collections.<PoolChunkMetric>emptyList().iterator();

    //ChunkList是在PoolArena中创建的，这里是表明该ChunkList所属的PoolArena
    private final PoolArena<T> arena;

    //PoolChunkList也会和其他的PoolChunkList构成链表，所以这里得到下一个PoolChunkList的指针
    private final PoolChunkList<T> nextList;

    //该list内每一个Chunk的最小内存利用率
    private final int minUsage;

    //该list内每一个Chunk的最大内存利用率
    private final int maxUsage;

    //该链表内每一个Chunk可以分配的最大内存值
    private final int maxCapacity;

    //该链表中的头节点
    private PoolChunk<T> head;

    //PoolChunkList也会和其他的PoolChunkList构成链表，所以这里得到前一个PoolChunkList的指针
    private PoolChunkList<T> prevList;

    /**
     * 构造函数，这里面几个参数的作用都讲过了，这里就不再重复了
     *
     * @param arena
     * @param nextList
     * @param minUsage
     * @param maxUsage
     * @param chunkSize
     */
    PoolChunkList(PoolArena<T> arena, PoolChunkList<T> nextList, int minUsage, int maxUsage, int chunkSize) {
        assert minUsage <= maxUsage;
        this.arena = arena;
        this.nextList = nextList;
        this.minUsage = minUsage;
        this.maxUsage = maxUsage;
        maxCapacity = calculateMaxCapacity(minUsage, chunkSize);
    }


    /**
     * 该方法可以得到每一个Chunk可以分配的最大内存值
     *
     * @param minUsage
     * @param chunkSize
     * @return
     */
    private static int calculateMaxCapacity(int minUsage, int chunkSize) {
        //最小内存使用率不能低于1
        minUsage = minUsage0(minUsage);
        if (minUsage == 100) {
            //如果等于100就不能分配任何内存了
            return 0;
        }
        //这个就是计算百分比的数学运算了
        //就是100-去最小利用值然后除以100，让chunkSize乘以这个百分数就行了
        return (int) (chunkSize * (100L - minUsage) / 100L);
    }


    /**
     * 分配内存的方法
     *
     * @param buf
     * @param reqCapacity
     * @param normCapacity
     * @return
     */
    boolean allocate(PooledByteBuf<T> buf, int reqCapacity, int normCapacity) {
        //如果要申请的内存超过了一个Chunk可分配的最大内存值
        if (normCapacity > maxCapacity) {
            //分配不了就直接退出
            return false;
        }
        //便遍历该链表中的Chunk
        for (PoolChunk<T> cur = head; cur != null; cur = cur.next) {
            //从Chunk中分配经过规整的内存，具体的方法都在PoolChunk中，这里我们知识粗讲逻辑
            //核心会在PoolChunk中讲到
            if (cur.allocate(buf, reqCapacity, normCapacity)) {
                //这里就会判断当前分配完内存的Chunk的内存利用率是否超过了它的最大内存利用率
                if (cur.usage() >= maxUsage) {
                    //超过了就从当前链表中移除该Chunk
                    remove(cur);
                    //把该Chunk添加到链表的下一个节点中
                    //注意，这里的下一个节点是PoolChunkList组成的链表的下一个节点
                    nextList.add(cur);
                }
                return true;
            }
        }
        return false;
    }


    /**
     * 释放内存的方法
     *
     * @param chunk
     * @param handle
     * @param nioBuffer
     * @return
     */
    boolean free(PoolChunk<T> chunk, long handle, ByteBuffer nioBuffer) {
        //把内存归还给它对应的Chunk内存块
        chunk.free(handle, nioBuffer);
        if (chunk.usage() < minUsage) {
            //判断当前Chunk内存块的最小内存使用率是否小于它的最小值
            //如果小于就把该Chunk内存块从该链表中移除
            remove(chunk);
            //将该Chunk内存块移动到PoolChunkList链表的上一个节点中
            return move0(chunk);
        }
        return true;
    }


    /**
     * 从链表中删除该Chunk节点，操作数据结构的方法，大家自己看看逻辑就行，我就不详细注释了
     * 这里只有一个是否头节点的区分，逻辑很简单
     * @param cur
     */
    private void remove(PoolChunk<T> cur) {
        if (cur == head) {
            head = cur.next;
            if (head != null) {
                head.prev = null;
            }
        } else {
            PoolChunk<T> next = cur.next;
            cur.prev.next = next;
            if (next != null) {
                next.prev = cur.prev;
            }
        }
    }


    /**
     * 真正移动Chunk内存块到PoolChunkList链表的上一个节点中的方法
     *
     * @param chunk
     * @return
     */
    private boolean move(PoolChunk<T> chunk) {
        //断言该Chunk的内存利用率小于该List对象的利用率最大值
        assert chunk.usage() < maxUsage;
        //如果该Chunk的内存利用率小于该List对象的利用率最小值
        if (chunk.usage() < minUsage) {
            //递归调用move0方法，继续向前驱节点移动
            return move0(chunk);
        }
        //把Chunk内存块添加到该List对象中
        add0(chunk);
        return true;
    }


    /**
     * 把Chunk内存块移动到上一个PoolChunkList节点中的方法
     * @param chunk
     * @return
     */
    private boolean move0(PoolChunk<T> chunk) {
        //判断是否有前驱节点
        if (prevList == null) {
            //如果没有前驱节点，大家可以想想，哪个PoolChunkList对象没有前驱节点？是q000对象
            assert chunk.usage() == 0;
            //这里直接返回说明如果这个Chunk内存块本身就在q000对象中了，没有前驱节点可以移动，它就没必要移动，等待被释放即可
            return false;
        }
        //走到这里说明有前驱节点，那就移动带前驱节点中
        return prevList.move(chunk);
    }

    /**
     * 真正把Chunk加入到PoolChunkList中的方法这里仍然是以是否有头节点为区分，逻辑很简单。
     * @param chunk
     */
    void add0(PoolChunk<T> chunk) {
        chunk.parent = this;
        if (head == null) {
            head = chunk;
            chunk.prev = null;
            chunk.next = null;
        } else {
            chunk.prev = null;
            chunk.next = head;
            head.prev = chunk;
            head = chunk;
        }
    }


    /**
     * 该方法就是用来把一个Chunk内存块添加到PoolChunkList中的方法
     * @param chunk
     */
    void add(PoolChunk<T> chunk) {
        //这里先判断一下内存的利用率，如果内存利用率超过了该PoolChunkList的最大内存利用率
        if (chunk.usage() >= maxUsage) {
            //就寻找链表中的下一个节点，然后把该Chunk尝试着放到下一个节点中
            //其实就是递归调用该方法
            nextList.add(chunk);
            return;
        }
        //这里意味着内存利用率符合要求，直接放到该链表中即可
        add0(chunk);
    }

    /**
     *
     * @param prevList
     */
    void prevList(PoolChunkList<T> prevList) {
        assert this.prevList == null;
        this.prevList = prevList;
    }

    @Override
    public int minUsage() {
        return minUsage0(minUsage);
    }

    @Override
    public int maxUsage() {
        return min(maxUsage, 100);
    }

    private static int minUsage0(int value) {
        return max(1, value);
    }


    @Override
    public Iterator<PoolChunkMetric> iterator() {
        synchronized (arena) {
            if (head == null) {
                return EMPTY_METRICS;
            }
            List<PoolChunkMetric> metrics = new ArrayList<PoolChunkMetric>();
            for (PoolChunk<T> cur = head; ; ) {
                metrics.add(cur);
                cur = cur.next;
                if (cur == null) {
                    break;
                }
            }
            return metrics.iterator();
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        synchronized (arena) {
            if (head == null) {
                return "none";
            }

            for (PoolChunk<T> cur = head; ; ) {
                buf.append(cur);
                cur = cur.next;
                if (cur == null) {
                    break;
                }
                buf.append(StringUtil.NEWLINE);
            }
        }
        return buf.toString();
    }

    void destroy(PoolArena<T> arena) {
        PoolChunk<T> chunk = head;
        while (chunk != null) {
            arena.destroyChunk(chunk);
            chunk = chunk.next;
        }
        head = null;
    }
}
