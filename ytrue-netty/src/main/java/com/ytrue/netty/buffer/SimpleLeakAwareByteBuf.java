package com.ytrue.netty.buffer;

import com.ytrue.netty.util.ResourceLeakTracker;
import com.ytrue.netty.util.internal.ObjectUtil;

import java.nio.ByteOrder;

/**
 * @author ytrue
 * @date 2023-08-10 9:20
 * @description :这个类型的对象对应内存检测级别为SIMPLE，从类中的众多方法可以看出，被该类包装的PooledDirectByteBuf，在每次调用
 * 方法时，并不会把调用轨迹记录下来。而被AdvancedLeakAwareByteBuf类包装的PooledDirectByteBuf，在执行它的方法时，会同时执行
 * recordLeakNonRefCountingOperation方法，将调用轨迹记录下来
 */
class SimpleLeakAwareByteBuf extends WrappedByteBuf {

    private final ByteBuf trackedByteBuf;
    final ResourceLeakTracker<ByteBuf> leak;

    SimpleLeakAwareByteBuf(ByteBuf wrapped, ByteBuf trackedByteBuf, ResourceLeakTracker<ByteBuf> leak) {
        super(wrapped);
        this.trackedByteBuf = ObjectUtil.checkNotNull(trackedByteBuf, "trackedByteBuf");
        this.leak = ObjectUtil.checkNotNull(leak, "leak");
    }

    SimpleLeakAwareByteBuf(ByteBuf wrapped, ResourceLeakTracker<ByteBuf> leak) {
        this(wrapped, wrapped, leak);
    }

    @Override
    public ByteBuf slice() {
        return newSharedLeakAwareByteBuf(super.slice());
    }

    @Override
    public ByteBuf retainedSlice() {
        return unwrappedDerived(super.retainedSlice());
    }

    @Override
    public ByteBuf retainedSlice(int index, int length) {
        return unwrappedDerived(super.retainedSlice(index, length));
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return unwrappedDerived(super.retainedDuplicate());
    }

    @Override
    public ByteBuf readRetainedSlice(int length) {
        return unwrappedDerived(super.readRetainedSlice(length));
    }

    @Override
    public ByteBuf slice(int index, int length) {
        return newSharedLeakAwareByteBuf(super.slice(index, length));
    }

    @Override
    public ByteBuf duplicate() {
        return newSharedLeakAwareByteBuf(super.duplicate());
    }

    @Override
    public ByteBuf readSlice(int length) {
        return newSharedLeakAwareByteBuf(super.readSlice(length));
    }

    @Override
    public ByteBuf asReadOnly() {
        return newSharedLeakAwareByteBuf(super.asReadOnly());
    }

    @Override
    public ByteBuf touch() {
        return this;
    }

    @Override
    public ByteBuf touch(Object hint) {
        return this;
    }

    /**
     * @Author: ytrue
     * @Description:在该方法内，会调用closeLeak方法，清除弱引用对象的引用，并且关闭对被包装的bytebuf的内存泄漏检测
     */
    @Override
    public boolean release() {
        if (super.release()) {
            closeLeak();
            return true;
        }
        return false;
    }

    @Override
    public boolean release(int decrement) {
        if (super.release(decrement)) {
            closeLeak();
            return true;
        }
        return false;
    }

    private void closeLeak() {
        //关闭对被包装的bytebuf的内存泄漏检测，在该方法内，还会把弱引用对象leak持有的引用清除干净
        boolean closed = leak.close(trackedByteBuf);
        assert closed;
    }

    @Override
    public ByteBuf order(ByteOrder endianness) {
        if (order() == endianness) {
            return this;
        } else {
            return newSharedLeakAwareByteBuf(super.order(endianness));
        }
    }

    //直接注释掉吧，这里就不再继续引入了
    private ByteBuf unwrappedDerived(ByteBuf derived) {
        ByteBuf unwrappedDerived = unwrapSwapped(derived);
//        if (unwrappedDerived instanceof AbstractPooledDerivedByteBuf) {
//            ((AbstractPooledDerivedByteBuf) unwrappedDerived).parent(this);
//            ResourceLeakTracker<ByteBuf> newLeak = AbstractByteBuf.leakDetector.track(derived);
//            if (newLeak == null) {
//                return derived;
//            }
//            return newLeakAwareByteBuf(derived, newLeak);
//        }
        return newSharedLeakAwareByteBuf(derived);
    }

    //直接注释掉吧。这里就不再继续引入了
    @SuppressWarnings("deprecation")
    private static ByteBuf unwrapSwapped(ByteBuf buf) {
//        if (buf instanceof SwappedByteBuf) {
//            do {
//                buf = buf.unwrap();
//            } while (buf instanceof SwappedByteBuf);
//
//            return buf;
//        }
        return buf;
    }

    private SimpleLeakAwareByteBuf newSharedLeakAwareByteBuf(
            ByteBuf wrapped) {
        return newLeakAwareByteBuf(wrapped, trackedByteBuf, leak);
    }

    private SimpleLeakAwareByteBuf newLeakAwareByteBuf(
            ByteBuf wrapped, ResourceLeakTracker<ByteBuf> leakTracker) {
        return newLeakAwareByteBuf(wrapped, wrapped, leakTracker);
    }

    protected SimpleLeakAwareByteBuf newLeakAwareByteBuf(
            ByteBuf buf, ByteBuf trackedByteBuf, ResourceLeakTracker<ByteBuf> leakTracker) {
        return new SimpleLeakAwareByteBuf(buf, trackedByteBuf, leakTracker);
    }
}
