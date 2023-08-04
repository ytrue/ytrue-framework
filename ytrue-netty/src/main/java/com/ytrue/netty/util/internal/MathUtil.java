package com.ytrue.netty.util.internal;

/**
 * @author ytrue
 * @date 2023-08-03 11:00
 * @description该类是从源码中直接复制过来的，一个简单的工具类
 */
public final class MathUtil {

    private MathUtil() {
    }

    public static int findNextPositivePowerOfTwo(final int value) {
        assert value > Integer.MIN_VALUE && value < 0x40000000;
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }


    public static int safeFindNextPositivePowerOfTwo(final int value) {
        return value <= 0 ? 1 : value >= 0x40000000 ? 0x40000000 : findNextPositivePowerOfTwo(value);
    }


    public static boolean isOutOfBounds(int index, int length, int capacity) {
        return (index | length | (index + length) | (capacity - (index + length))) < 0;
    }


    public static int compare(final int x, final int y) {
        // do not subtract for comparison, it could overflow
        return x < y ? -1 : (x > y ? 1 : 0);
    }


    public static int compare(long x, long y) {
        return (x < y) ? -1 : (x > y) ? 1 : 0;
    }
}
