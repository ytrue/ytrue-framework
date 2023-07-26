package com.ytrue.netty.channel;

/**
 * @author ytrue
 * @date 2023-07-26 9:04
 * @description 这个类中调用了很多操作系统方法，我就不引进这个了，引入这个类就需要引入更多不必要的类。所以我直接用最简单的式代替吧，就搞一个时间戳作为channelId
 */
public class DefaultChannelId  implements ChannelId {

    private String longValue;

    private static final long serialVersionUID = 3884076183504074063L;


    public static DefaultChannelId newInstance() {
        return new DefaultChannelId();
    }

    private DefaultChannelId() {
        long currentTimeMillis = System.currentTimeMillis();
        this.longValue = String.valueOf(currentTimeMillis);
    }

    @Override
    public String asShortText() {
        return null;
    }

    @Override
    public String asLongText() {
        String longValue = this.longValue;
        if (longValue == null) {
            this.longValue = longValue =String.valueOf(System.currentTimeMillis());
        }
        return longValue;
    }

    @Override
    public String toString() {
        return asShortText();
    }

    @Override
    public int compareTo(ChannelId o) {
        return 0;
    }
}
