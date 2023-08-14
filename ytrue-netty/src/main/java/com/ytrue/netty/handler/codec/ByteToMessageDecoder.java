package com.ytrue.netty.handler.codec;

import com.ytrue.netty.buffer.ByteBuf;
import com.ytrue.netty.buffer.ByteBufAllocator;
import com.ytrue.netty.channel.ChannelHandlerContext;
import com.ytrue.netty.channel.ChannelInboundHandlerAdapter;
import com.ytrue.netty.util.internal.StringUtil;

import java.util.List;

import static com.ytrue.netty.util.internal.ObjectUtil.checkPositive;

/**
 * @author ytrue
 * @date 2023-08-14 14:37
 * @description Netty解码器的核心类，把ByteBuf解码成用户自己设定的对象，这里要为大家强调一点的是，解码器是有状态的，这就意味着它
 *  不可能被channel共享，每一个channel都要有自己的解码器handler
 */
public abstract class ByteToMessageDecoder extends ChannelInboundHandlerAdapter {

    /**
     * @Author: ytrue
     * @Description:这个就是解码器中的累加器，当接收到的数据还不足以解码成一个完整的消息时，接收到的数据会
     * 暂时存储在这个属性中
     */
    public static final Cumulator MERGE_CUMULATOR = new Cumulator() {
        //这里要多说一句，第一次解码消息的时候，并不会调用这个方法，因为还不知道能不能解码成一个完整的消息
        //之后再次解码，才会调用该方法，如果之前接收到的字节不能解码成完整的消息，就会存储在这个cumulation对象中
        //这个对象就是可累加的缓冲区
        @Override
        public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
            try {
                final ByteBuf buffer;
                //这里就意味着cumulation的容量不够了，需要扩容，其实writerIndex和readableBytes都是用到了写指针
                //其实下面这个不等式可以变成cumulation.writerIndex()+in.readableBytes()>cumulation.maxCapacity()
                //两部分要存储起来的字节已经超过了cumulation的最大容量，当然就要扩容了
                if (cumulation.writerIndex() > cumulation.maxCapacity() - in.readableBytes()
                    || cumulation.refCnt() > 1 || cumulation.isReadOnly()) {
                    //在这里扩容，返回一个新的byteBuf
                    buffer = expandCumulation(alloc, cumulation, in.readableBytes());
                } else {
                    //如果走这个分支，就使用原来的ByteBuf
                    buffer = cumulation;
                }
                //把接收到的字节从缓冲区写入到累加器中
                buffer.writeBytes(in);
                return buffer;
            } finally {
                //释放用来接收消息的ByteBuf
                in.release();
            }
        }
    };


    public static final Cumulator COMPOSITE_CUMULATOR = new Cumulator() {
        @Override
        public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
            ByteBuf buffer;
            try {
                if (cumulation.refCnt() > 1) {
                    buffer = expandCumulation(alloc, cumulation, in.readableBytes());
                    buffer.writeBytes(in);
                } else {
                    //CompositeByteBuf没有引入，所以这里直接注释掉了
//                    CompositeByteBuf composite;
//                    if (cumulation instanceof CompositeByteBuf) {
//                        composite = (CompositeByteBuf) cumulation;
//                    } else {
//                        composite = alloc.compositeBuffer(Integer.MAX_VALUE);
//                        composite.addComponent(true, cumulation);
//                    }
//                    composite.addComponent(true, in);
//                    in = null;
//                    buffer = composite;
                    return null;
                }
                return buffer;
            } finally {
                if (in != null) {
                    in.release();
                }
            }
        }
    };

    //解码器中其实有状态转换的，下面这个属性就是初始状态
    private static final byte STATE_INIT = 0;
    //这个状态意味着解码器正在被使用
    private static final byte STATE_CALLING_CHILD_DECODE = 1;
    //这个状态意味着解码器即将被删除
    private static final byte STATE_HANDLER_REMOVED_PENDING = 2;
    //这个属性就是可累加的缓冲区
    ByteBuf cumulation;
    //向累加缓冲区写入数据的累加器
    private Cumulator cumulator = MERGE_CUMULATOR;
    //是否只解码一次
    private boolean singleDecode;
    //是否第一次使用累加缓冲区
    private boolean first;
    //这个成员变量和channel配置类中的是否自动读有关，但在我们这里不是重点
    private boolean firedChannelRead;
    //解码器的初始状态就是STATE_INIT
    private byte decodeState = STATE_INIT;
    //这个属性下面会用到，就是当接收到的消息非常多，解码很多次也没有解码完整
    //就不会再继续让这些消息占用内存了，就会把它们丢弃掉，下面这个值就是用来确定
    //从什么时候开始丢弃的
    private int discardAfterReads = 16;
    //这个是解码器处理消息的次数，就是读取了多少次消息的意思，会和上面的16做
    //判断，如果超过16次，消息还没解码完整，就可以丢弃字节了
    private int numReads;

    protected ByteToMessageDecoder() {
        ensureNotSharable();
    }


    public void setSingleDecode(boolean singleDecode) {
        this.singleDecode = singleDecode;
    }


    public boolean isSingleDecode() {
        return singleDecode;
    }


    public void setCumulator(Cumulator cumulator) {
        if (cumulator == null) {
            throw new NullPointerException("cumulator");
        }
        this.cumulator = cumulator;
    }


    public void setDiscardAfterReads(int discardAfterReads) {
        checkPositive(discardAfterReads, "discardAfterReads");
        this.discardAfterReads = discardAfterReads;
    }


    protected int actualReadableBytes() {
        return internalBuffer().readableBytes();
    }


    protected ByteBuf internalBuffer() {
        if (cumulation != null) {
            return cumulation;
        } else {
            return null;
        }
    }

    @Override
    public final void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (decodeState == STATE_CALLING_CHILD_DECODE) {
            decodeState = STATE_HANDLER_REMOVED_PENDING;
            return;
        }
        ByteBuf buf = cumulation;
        if (buf != null) {
            cumulation = null;
            numReads = 0;
            int readable = buf.readableBytes();
            if (readable > 0) {
                ByteBuf bytes = buf.readBytes(readable);
                buf.release();
                //把数据传出去
                ctx.fireChannelRead(bytes);
                ctx.fireChannelReadComplete();
            } else {
                buf.release();
            }
        }
        handlerRemoved0(ctx);
    }


    protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception { }

    /**
     * @Author: ytrue
     * @Description:这个就是解码器中最核心的方法
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //这里就可以大胆地判断类型了，因为接收消息时都会把字节存放在ByteBuf中
        //所以这里要判断是不是ByteBuf类型的
        if (msg instanceof ByteBuf) {
            //这里得到的CodecOutputList类型很有意思，它存放的其实就是暂时解码了的消息
            //使用其内部的数组来存放消息对象
            CodecOutputList out = CodecOutputList.newInstance();
            try {
                //转换类型，
                ByteBuf data = (ByteBuf) msg;
                //如果是第一次接收到消息并解码，这时候累加缓冲区肯定还没用到，得等到第一次解码完了才能知道
                //消息是否完整，所以第一次就把累加缓冲区设置为null
                first = cumulation == null;
                if (first) {
                    //直接赋值即可，都是ByteBuf类型的对象
                    cumulation = data;
                } else {
                    //走到这里就意味着不是第一次解码，所以要把接收到的字节通过累加器写入到累加缓冲区中
                    cumulation = cumulator.cumulate(ctx.alloc(), cumulation, data);
                }
                //在这里进行解码，解码完毕后，会把累加缓冲区中的字节存放到out对象的数组中，然后传递给后面的handler
                callDecode(ctx, cumulation, out);
            } catch (DecoderException e) {
                throw e;
            } catch (Exception e) {
                throw new DecoderException(e);
            } finally {
                //这里会有判断，就是判断累加缓冲区不为null，说明已经不是第一次解码消息了
                //并且累加缓冲区中没有可读的字节数，这种情况就说明累加缓冲区已经处理过消息，并且消息已经存放到out对象或者是
                //直接传递给后面的处理器中了，这就意味着这一次解码消息已经完整了，可以放心地释放各种ByteBuf对象了
                if (cumulation != null && !cumulation.isReadable()) {
                    //处理消息的次数清0
                    numReads = 0;
                    //释放缓冲区
                    cumulation.release();
                    //帮助垃圾回收
                    cumulation = null;
                    //走到这里就意味着处理消息的次数已经超过16次了，比如解码一条消息，每一次接收到的字节都不能将消息拼凑完整
                    //处理了16次了，这时候就要直接丢弃读到的数据了，考虑到会占用很多内存
                    //这其实也会提醒我们，一次发送太多消息，可能Netty就不会在接受的过程中丢弃一部分
                } else if (++ numReads >= discardAfterReads) {
                    //清0次数
                    numReads = 0;
                    //丢弃处理过的字节数
                    discardSomeReadBytes();
                }
                int size = out.size();
                firedChannelRead |= out.insertSinceRecycled();
                //解码完成了，就把消息从out对象中向后面的handler传递
                fireChannelRead(ctx, out, size);
                //释放out对象，out对象也是有对象池的
                out.recycle();
            }
        } else {
            //类型不匹配，就传给下一个handler
            ctx.fireChannelRead(msg);
        }
    }


    static void fireChannelRead(ChannelHandlerContext ctx, List<Object> msgs, int numElements) {
        if (msgs instanceof CodecOutputList) {
            fireChannelRead(ctx, (CodecOutputList) msgs, numElements);
        } else {
            for (int i = 0; i < numElements; i++) {
                ctx.fireChannelRead(msgs.get(i));
            }
        }
    }


    static void fireChannelRead(ChannelHandlerContext ctx, CodecOutputList msgs, int numElements) {
        for (int i = 0; i < numElements; i ++) {
            ctx.fireChannelRead(msgs.getUnsafe(i));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        numReads = 0;
        discardSomeReadBytes();
        if (!firedChannelRead && !ctx.channel().config().isAutoRead()) {
            ctx.read();
        }
        firedChannelRead = false;
        ctx.fireChannelReadComplete();
    }

    protected final void discardSomeReadBytes() {
        if (cumulation != null && !first && cumulation.refCnt() == 1) {
            cumulation.discardSomeReadBytes();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelInputClosed(ctx, true);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        if (evt instanceof ChannelInputShutdownEvent) {
//            channelInputClosed(ctx, false);
//        }
        super.userEventTriggered(ctx, evt);
    }

    private void channelInputClosed(ChannelHandlerContext ctx, boolean callChannelInactive) throws Exception {
        CodecOutputList out = CodecOutputList.newInstance();
        try {
            channelInputClosed(ctx, out);
        } catch (DecoderException e) {
            throw e;
        } catch (Exception e) {
            throw new DecoderException(e);
        } finally {
            try {
                if (cumulation != null) {
                    cumulation.release();
                    cumulation = null;
                }
                int size = out.size();
                fireChannelRead(ctx, out, size);
                if (size > 0) {
                    // Something was read, call fireChannelReadComplete()
                    ctx.fireChannelReadComplete();
                }
                if (callChannelInactive) {
                    ctx.fireChannelInactive();
                }
            } finally {
                // Recycle in all cases
                out.recycle();
            }
        }
    }


    void channelInputClosed(ChannelHandlerContext ctx, List<Object> out) throws Exception {
        if (cumulation != null) {
            callDecode(ctx, cumulation, out);
            decodeLast(ctx, cumulation, out);
        } else {
            //decodeLast(ctx, Unpooled.EMPTY_BUFFER, out);
        }
    }

    /**
     * @Author: ytrue
     * @Description:解码消息的方法
     */
    protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            //在一个循环中处理接收到的字节消息
            while (in.isReadable()) {
                //得到out对象内存储消息对象的个数
                int outSize = out.size();
                //如果大于0，意味着之前解码到了完整的消息，可以继续向后面的handler传递了
                //这里还是要结合具体的编解码器来理解的
                if (outSize > 0) {
                    //向后传递消息
                    fireChannelRead(ctx, out, outSize);
                    //把out对象内存储的消息对象清空
                    out.clear();
                    //判断该解码器handler是否被删除
                    if (ctx.isRemoved()) {
                        break;
                    }
                    outSize = 0;
                }
                //走到这里说明outSize中还未存储消息对象
                //这可能就意味着解码后的消息一直不完整，没办法从累加缓冲区中存放到out对象中
                //这里也要结合具体的解码器来理解。我会在本节课的test包中为大家写两个具体的编解码器实现类
                //结合具体的例子，大家就明白这里是什么意思了
                //这里是得到可以读到的所有字节数
                int oldInputLength = in.readableBytes();

                //在下面的方法中真正解码
                decodeRemovalReentryProtection(ctx, in, out);
                //判断这个handler是否删除了
                if (ctx.isRemoved()) {
                    break;
                }
                //走到这里说明已经经过解码了，但是大家要注意，虽然解码了，但是out对象中未必有数据，因为
                //解码后的消息不完整，是不会把消息对象存入到out对象中的
                //所以很有可能已经解码消息了，但是out对象中的消息对象还是0，上面代码的逻辑必然会让out对象赋值为0
                //但还有一种情况，也可能根本就没有消息了，消息已经解码完整了并且发送给下一个handler了，
                //下一轮循环时没有消息可解码了
                if (outSize == out.size()) {
                    //所以这里要判断一下，累加缓冲区中的可读字节数是否有变化
                    if (oldInputLength == in.readableBytes()) {
                        //如果没变化，说明没有消息了，直接退出即可
                        //或者是消息根本不完整，直接退出循环，在上一层方法中等待累加器向累加缓冲区中写入更多字节
                        break;
                    } else {
                        //有变化则继续循环即可
                        continue;
                    }
                }
                //走到这里说明out对象中的size不为0，就意味着肯定解码成功了
                //但是累加缓冲区中的可读字节书没变化，肯定就出问题了，抛出异常即可
                if (oldInputLength == in.readableBytes()) {
                    throw new DecoderException(
                            StringUtil.simpleClassName(getClass()) +
                            ".decode() did not read anything but decoded a message.");
                }
                //如果只解码一次，则循环一次直接退出即可
                if (isSingleDecode()) {
                    break;
                }
            }
        } catch (DecoderException e) {
            throw e;
        } catch (Exception cause) {
            throw new DecoderException(cause);
        }
    }


    /**
     * @Author: ytrue
     * @Description:解码的抽象方法，交给具体的子类来实现
     */
    protected abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception;


    final void decodeRemovalReentryProtection(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
            throws Exception {
        //解码器的状态设置为正在被使用
        decodeState = STATE_CALLING_CHILD_DECODE;
        try {
            //真正开始解码
            decode(ctx, in, out);
        } finally {
            //判断解码器状态是否为待删除
            boolean removePending = decodeState == STATE_HANDLER_REMOVED_PENDING;
            //设置为初始化，因为该解码器本次已经使用完了
            decodeState = STATE_INIT;
            if (removePending) {
                //如果是待删除状态，就直接删除
                handlerRemoved(ctx);
            }
        }
    }


    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.isReadable()) {
            decodeRemovalReentryProtection(ctx, in, out);
        }
    }

    static ByteBuf expandCumulation(ByteBufAllocator alloc, ByteBuf cumulation, int readable) {
        ByteBuf oldCumulation = cumulation;
        cumulation = alloc.buffer(oldCumulation.readableBytes() + readable);
        cumulation.writeBytes(oldCumulation);
        oldCumulation.release();
        return cumulation;
    }


    public interface Cumulator {

        ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in);
    }
}
