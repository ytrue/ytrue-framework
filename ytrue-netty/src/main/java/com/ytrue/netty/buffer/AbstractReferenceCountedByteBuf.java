package com.ytrue.netty.buffer;

import com.ytrue.netty.util.internal.ReferenceCountUpdater;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author ytrue
 * @date 2023-08-07 11:25
 * @description 引用计数接口的实现类
 */
public abstract class AbstractReferenceCountedByteBuf extends AbstractByteBuf {

    /**
     * @Author: ytrue
     * @Description:得到refCnt属性在该类中的内存偏移量
     */
    private static final long REFCNT_FIELD_OFFSET =
            ReferenceCountUpdater.getUnsafeOffset(AbstractReferenceCountedByteBuf.class, "refCnt");

    /**
     * @Author: ytrue
     * @Description:refCnt的原子更新器
     */
    private static final AtomicIntegerFieldUpdater<AbstractReferenceCountedByteBuf> AIF_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractReferenceCountedByteBuf.class, "refCnt");

    /**
     * @Author: ytrue
     * @Description:创建了一个引用计数的更新器，但是真正的更新还是原子更新器来做，因为把AIF_UPDATER传到对象中了
     */
    private static final ReferenceCountUpdater<AbstractReferenceCountedByteBuf> updater =
            new ReferenceCountUpdater<AbstractReferenceCountedByteBuf>() {
                @Override
                protected AtomicIntegerFieldUpdater<AbstractReferenceCountedByteBuf> updater() {
                    return AIF_UPDATER;
                }
                @Override
                protected long unsafeOffset() {
                    return REFCNT_FIELD_OFFSET;
                }
            };


    //refCnt引用计数，初始值为2，这里的这个2实际上是个虚数，简单来说，就是一个对象每被引用一次，refCnt就会加2。
    @SuppressWarnings("unused")
    private volatile int refCnt = updater.initialValue();

    protected AbstractReferenceCountedByteBuf(int maxCapacity) {
        super(maxCapacity);
    }

    /**
     * @Author: ytrue
     * @Description:对象是否可被访问
     */
    @Override
    boolean isAccessible() {
        return updater.isLiveNonVolatile(this);
    }

    /**
     * @Author: ytrue
     * @Description:得到引用计数的值
     */
    @Override
    public int refCnt() {
        return updater.refCnt(this);
    }


    protected final void setRefCnt(int refCnt) {
        updater.setRefCnt(this, refCnt);
    }


    protected final void resetRefCnt() {
        updater.resetRefCnt(this);
    }

    //引用增加的方法
    @Override
    public ByteBuf retain() {
        return updater.retain(this);
    }

    @Override
    public ByteBuf retain(int increment) {
        return updater.retain(this, increment);
    }

    @Override
    public ByteBuf touch() {
        return this;
    }

    @Override
    public ByteBuf touch(Object hint) {
        return this;
    }

    //引用减少的方法
    @Override
    public boolean release() {
        return handleRelease(updater.release(this));
    }

    @Override
    public boolean release(int decrement) {
        return handleRelease(updater.release(this, decrement));
    }

    /**
     * @Author: ytrue
     * @Description:回收对象的方法，这里会调用到deallocate抽象方法，该方法会在每个不同的ByteBuf实现类中得到实现
     * 也就是真正回收对象的方法，把对象放回到对象池
     */
    private boolean handleRelease(boolean result) {
        //判断是否可以回收了
        if (result) {
            deallocate();
        }
        return result;
    }

    protected abstract void deallocate();
}
