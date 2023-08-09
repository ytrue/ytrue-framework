package com.ytrue.netty.util.internal;

import com.ytrue.netty.util.IllegalReferenceCountException;
import com.ytrue.netty.util.ReferenceCounted;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static com.ytrue.netty.util.internal.ObjectUtil.checkPositive;

/**
 * @author ytrue
 * @date 2023-08-07 11:53
 * @description ReferenceCountUpdater
 */
public abstract class ReferenceCountUpdater<T extends ReferenceCounted> {


    protected ReferenceCountUpdater() {
    }

    /**
     * @Author: ytrue
     * @Description:获取一个类中的某个成员变量的内存偏移量
     */
    public static long getUnsafeOffset(Class<? extends ReferenceCounted> clz, String fieldName) {
        try {
            if (PlatformDependent.hasUnsafe()) {
                return PlatformDependent.objectFieldOffset(clz.getDeclaredField(fieldName));
            }
        } catch (Throwable ignore) {
            // fall-back
        }
        return -1;
    }

    /**
     * @Author: ytrue
     * @Description:原子更新器，会从AbstractReferenceCountedByteBuf类中传递过来
     */
    protected abstract AtomicIntegerFieldUpdater<T> updater();

    /**
     * @Author: ytrue
     * @Description:引用计数的内存偏移量，也是从AbstractReferenceCountedByteBuf类中传递过来的
     */
    protected abstract long unsafeOffset();

    /**
     * @Author: ytrue
     * @Description:引用计数的初始值，我们之前说过，它是一个虚值，每多一个引用，引用计数就加2 为什么要让初始值是2，并且在没有引用计数的时候把改值设置为1，这个和多线程并发的情况有关。
     */
    public final int initialValue() {
        return 2;
    }

    /**
     * @Author: ytrue
     * @Description:返回引用计数的值，这里做了个小的设计，尤其是rawCnt & 1这行代码，和1做与运算
     * 实际上就是一个数与上00000001，看的也就是最后一位，如果和1做位运算的那个数的最后一位是1，做位运算
     * 得到的结果就一定不是0，这种情况说明那个数一定是个奇数，如果rawCnt是奇数，那就直接返回0。
     * 为什么要这么做呢？因为之前我们说了引用计数的初始值是2，但这个是虚值，每多一个引用就加2，这就意味着只要对象被引用着，
     * 返回的就一定是个偶数，如果rawCnt是奇数，那就直接返回0。在这里就是判断了一下rawCnt是奇数还是偶数，如果是奇数就直接返回0
     * 如果是偶数，就返回rawCnt除以2的值
     */
    private static int realRefCnt(int rawCnt) {
        return rawCnt != 2 && rawCnt != 4 && (rawCnt & 1) != 0 ? 0 : rawCnt >>> 1;
    }


    /**
     * @Author: ytrue
     * @Description:这个也是得到引用计数的真实值，只不过这里如果rawCnt是奇数就直接抛出异常，并且是用在修改引用计数的时候的方法
     */
    private static int toLiveRealRefCnt(int rawCnt, int decrement) {
        if (rawCnt == 2 || rawCnt == 4 || (rawCnt & 1) == 0) {
            return rawCnt >>> 1;
        }
        throw new IllegalReferenceCountException(0, -decrement);
    }

    private int nonVolatileRawCnt(T instance) {
        // TODO: Once we compile against later versions of Java we can replace the Unsafe usage here by varhandles.
        final long offset = unsafeOffset();
        return offset != -1 ? PlatformDependent.getInt(instance, offset) : updater().get(instance);
    }

    /**
     * @Author: ytrue
     * @Description:得到引用计数的方法
     */
    public final int refCnt(T instance) {
        //得到refCnt，但这个是虚值，所以要传到下面这个方法内判断是不是偶数，如果是就返回除以2的值
        return realRefCnt(updater().get(instance));
    }

    /**
     * @Author: ytrue
     * @Description:非Volatile的方式获取引用计数
     */
    public final boolean isLiveNonVolatile(T instance) {
        final long offset = unsafeOffset();
        final int rawCnt = offset != -1 ? PlatformDependent.getInt(instance, offset) : updater().get(instance);
        return rawCnt == 2 || rawCnt == 4 || rawCnt == 6 || rawCnt == 8 || (rawCnt & 1) == 0;
    }


    public final void setRefCnt(T instance, int refCnt) {
        updater().set(instance, refCnt > 0 ? refCnt << 1 : 1);
    }


    public final void resetRefCnt(T instance) {
        updater().set(instance, initialValue());
    }

    /**
     * @Author: ytrue
     * @Description:增加引用计数的方法
     */
    public final T retain(T instance) {
        return retain0(instance, 1, 2);
    }

    /**
     * @Author: ytrue
     * @Description:增加引用计数的方法
     */
    public final T retain(T instance, int increment) {
        //这里是把要增加的计数乘以2，因为要增加的虚值是以2为基数增加的
        int rawIncrement = checkPositive(increment, "increment") << 1;
        return retain0(instance, increment, rawIncrement);
    }

    /**
     * @Author: ytrue
     * @Description:增加引用计数的真正方法
     */
    private T retain0(T instance, final int increment, final int rawIncrement) {
        //得到旧值，并且给旧值加上rawIncrement，这个就是2
        int oldRef = updater().getAndAdd(instance, rawIncrement);
        //这里又做了此判断，如果旧值不是偶数就抛出异常
        if (oldRef != 2 && oldRef != 4 && (oldRef & 1) != 0) {
            throw new IllegalReferenceCountException(0, increment);
        }
        //这里是判断旧值太大超过了int的最大范围或者是旧值加上增加的值结果太大，超过了int范围
        //就会变成负数，如果超过了就要减去刚才加上的值，然后抛出异常
        if ((oldRef <= 0 && oldRef + rawIncrement >= 0) || (oldRef >= 0 && oldRef + rawIncrement < oldRef)) {
            updater().getAndAdd(instance, -rawIncrement);
            throw new IllegalReferenceCountException(realRefCnt(oldRef), increment);
        }
        return instance;
    }

    /**
     * @Author: ytrue
     * @Description:减少引用计数的方法
     */
    public final boolean release(T instance) {
        //得到旧的引用计数值
        int rawCnt = nonVolatileRawCnt(instance);
        //这里判断引用计数是否为2实际上就是判断该对象是否只被引用一次，如果引用一次，那直接把引用计数修改为1即可。
        //如果失败就用retryRelease0方法循环重试，直到成功
        return rawCnt == 2 ? tryFinalRelease0(instance, 2) || retryRelease0(instance, 1)
                //走到这里意味着对象还被多次引用，所以更新引用计数的虚值为引用计数虚值减去要减去的值乘以2
                : nonFinalRelease0(instance, 1, rawCnt, toLiveRealRefCnt(rawCnt, 1));
    }

    /**
     * @Author: ytrue
     * @Description:减少引用计数的方法，是否可以被回收
     */
    public final boolean release(T instance, int decrement) {
        //得到旧的引用计数值
        int rawCnt = nonVolatileRawCnt(instance);
        //获取引用计数的真实值，就是除以2之后的值
        int realCnt = toLiveRealRefCnt(rawCnt, checkPositive(decrement, "decrement"));
        //这里的是先判断真实的引用计数值是否等于就要减去的引用计数值，如果相等那就说明对象不被引用了，所以直接把引用计数的虚值置为1
        ////如果失败就用retryRelease0方法循环重试，直到成功
        return decrement == realCnt ? tryFinalRelease0(instance, rawCnt) || retryRelease0(instance, decrement)
                //这里是引用计数的真实值并不等于要减去的值，所以更新引用计数的虚值为引用计数虚值减去要减去的值乘以2
                : nonFinalRelease0(instance, decrement, rawCnt, realCnt);
    }

    /**
     * @Author: ytrue
     * @Description:原子更新引用计数，把引用计数的虚值置为1
     */
    private boolean tryFinalRelease0(T instance, int expectRawCnt) {
        return updater().compareAndSet(instance, expectRawCnt, 1);
    }

    /**
     * @Author: ytrue
     * @Description:原子更新引用计数的虚值，decrement << 1这行代码就是要减去的值乘以2，然后让引用计数的虚值减去。
     */
    private boolean nonFinalRelease0(T instance, int decrement, int rawCnt, int realCnt) {
        //先做一个判断，保证要减去的值小于引用计数的实际的值
        if (decrement < realCnt && updater().compareAndSet(instance, rawCnt, rawCnt - (decrement << 1))) {
            //返回fasle的意思是还有引用，所以不能被释放，所以才返回false
            return false;
        }
        //如果失败就循环更新，直到成功为止
        return retryRelease0(instance, decrement);
    }

    /**
     * @Author: ytrue
     * @Description:原子重复更新引用计数虚值的方法，更新的具体内容是减少引用计数虚值
     */
    private boolean retryRelease0(T instance, int decrement) {
        for (; ; ) {
            //得到引用计数的虚值，得到引用计数的真实的值
            int rawCnt = updater().get(instance), realCnt = toLiveRealRefCnt(rawCnt, decrement);
            //比较要减去的值是否等于引用计数的真实的值
            if (decrement == realCnt) {
                //如果等于就直接把引用计数的虚值置为1
                if (tryFinalRelease0(instance, rawCnt)) {
                    //返回true说明对象可以被回收了
                    return true;
                }
                //如果要减去的值小于引用计数的实际的值
            } else if (decrement < realCnt) {
                //原子更新引用计数的虚值，就是减去decrement乘以2
                if (updater().compareAndSet(instance, rawCnt, rawCnt - (decrement << 1))) {
                    //返回false说明对象还不能被回收
                    return false;
                }
            } else {
                //走到这里说明数值有误，要抛出异常
                throw new IllegalReferenceCountException(realCnt, -decrement);
            }
            Thread.yield();
        }
    }
}
