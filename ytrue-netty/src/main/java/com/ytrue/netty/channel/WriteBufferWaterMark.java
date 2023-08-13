package com.ytrue.netty.channel;

import static com.ytrue.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * @author ytrue
 * @date 2023-08-13 9:37
 * @description 数据写入socket时的高低水位线类
 */
public final class WriteBufferWaterMark {

    //下面这两个属性就是水位线的高位和水位线的低位
    //ChannelOutboundBuffer写缓冲队列中的高水位线就是默认的64KB，而低水位线就是默认的32KB
    //这就意味着当发送的数据超过了高水位线，channel就不能再继续发送数据了
    //而低于32KB时，channel就可以发送数据
    private static final int DEFAULT_LOW_WATER_MARK = 32 * 1024;
    private static final int DEFAULT_HIGH_WATER_MARK = 64 * 1024;

    public static final WriteBufferWaterMark DEFAULT =
            new WriteBufferWaterMark(DEFAULT_LOW_WATER_MARK, DEFAULT_HIGH_WATER_MARK, false);

    private final int low;
    private final int high;


    public WriteBufferWaterMark(int low, int high) {
        this(low, high, true);
    }


    WriteBufferWaterMark(int low, int high, boolean validate) {
        if (validate) {
            checkPositiveOrZero(low, "low");
            if (high < low) {
                throw new IllegalArgumentException(
                        "write buffer's high water mark cannot be less than " +
                        " low water mark (" + low + "): " +
                        high);
            }
        }
        this.low = low;
        this.high = high;
    }


    public int low() {
        return low;
    }


    public int high() {
        return high;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(55)
                .append("WriteBufferWaterMark(low: ")
                .append(low)
                .append(", high: ")
                .append(high)
                .append(")");
        return builder.toString();
    }

}
