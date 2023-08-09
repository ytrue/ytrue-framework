package com.ytrue.netty.buffer;

import com.ytrue.netty.util.internal.StringUtil;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-09 14:40
 * @description PooledByteBufAllocatorMetric
 */
public final class PooledByteBufAllocatorMetric implements ByteBufAllocatorMetric {

    private final PooledByteBufAllocator allocator;

    PooledByteBufAllocatorMetric(PooledByteBufAllocator allocator) {
        this.allocator = allocator;
    }


    public int numHeapArenas() {
        return allocator.numHeapArenas();
    }


    public int numDirectArenas() {
        return allocator.numDirectArenas();
    }


    public List<PoolArenaMetric> heapArenas() {
        return allocator.heapArenas();
    }


    public List<PoolArenaMetric> directArenas() {
        return allocator.directArenas();
    }


    public int numThreadLocalCaches() {
        return allocator.numThreadLocalCaches();
    }


    public int tinyCacheSize() {
        return allocator.tinyCacheSize();
    }


    public int smallCacheSize() {
        return allocator.smallCacheSize();
    }


    public int normalCacheSize() {
        return allocator.normalCacheSize();
    }


    public int chunkSize() {
        return allocator.chunkSize();
    }

    @Override
    public long usedHeapMemory() {
        return allocator.usedHeapMemory();
    }

    @Override
    public long usedDirectMemory() {
        return allocator.usedDirectMemory();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append(StringUtil.simpleClassName(this))
                .append("(usedHeapMemory: ").append(usedHeapMemory())
                .append("; usedDirectMemory: ").append(usedDirectMemory())
                .append("; numHeapArenas: ").append(numHeapArenas())
                .append("; numDirectArenas: ").append(numDirectArenas())
                .append("; tinyCacheSize: ").append(tinyCacheSize())
                .append("; smallCacheSize: ").append(smallCacheSize())
                .append("; normalCacheSize: ").append(normalCacheSize())
                .append("; numThreadLocalCaches: ").append(numThreadLocalCaches())
                .append("; chunkSize: ").append(chunkSize()).append(')');
        return sb.toString();
    }
}
