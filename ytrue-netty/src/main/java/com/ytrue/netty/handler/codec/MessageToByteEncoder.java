package com.ytrue.netty.handler.codec;

import com.ytrue.netty.buffer.ByteBuf;
import com.ytrue.netty.channel.ChannelHandlerContext;
import com.ytrue.netty.channel.ChannelOutboundHandlerAdapter;
import com.ytrue.netty.channel.ChannelPromise;
import com.ytrue.netty.util.ReferenceCountUtil;
import com.ytrue.netty.util.internal.TypeParameterMatcher;

/**
 * @author ytrue
 * @date 2023-08-14 14:29
 * @description 这个就是Netty的核心编码器之一，大家注意，这个编码器是有泛型的，这一点和解码器不同
 * 解码器只需要字节数组即可，但是编码器需要明确把哪种类型的对象进行编码，进而创建对应的类型匹配器
 * 类型匹配器是编码器中很重要的属性
 */
public abstract class MessageToByteEncoder<I> extends ChannelOutboundHandlerAdapter {

    //这个成员变量就是类型匹配器，用它来判断将要编码的对象是否和类中的泛型一致
    //这个属性在UnpaddedInternalThreadLocalMap中被保存了
    private final TypeParameterMatcher matcher;
    //使用的是否为直接内存
    private final boolean preferDirect;


    //这个构造函数没有把要编码的类型传进来，所以内部会使用反射得到要编码的类型，然后再创建对应的类型匹配起
    protected MessageToByteEncoder() {
        this(true);
    }


    //这个构造器就是会把要编码的类型传进来，然后创建对应的类型匹配器
    protected MessageToByteEncoder(Class<? extends I> outboundMessageType) {
        this(outboundMessageType, true);
    }


    protected MessageToByteEncoder(boolean preferDirect) {
        //通过find方法得到对应的类型匹配器
        matcher = TypeParameterMatcher.find(this, MessageToByteEncoder.class, "I");
        this.preferDirect = preferDirect;
    }


    protected MessageToByteEncoder(Class<? extends I> outboundMessageType, boolean preferDirect) {
        //通过get方法生成对应类型的匹配器
        matcher = TypeParameterMatcher.get(outboundMessageType);
        this.preferDirect = preferDirect;
    }


    /**
     * @Author: PP-jessica
     * @Description:检查要编码的类型是否和泛型类型匹配的方法
     */
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    /**
     * @Author: PP-jessica
     * @Description:编码的方法，这里我想跟大家多说一句，请大家回想一下在Netty中是怎么发送消息的
     * 在常规项目中，比如用Netty进行通信等等，我们回自己定义请求和响应的编解码器，对吧？我们通过
     * writeAndFlush方法发送的其实就是一个request类型的对象，然后这个对象会经过编码器，在编码器中
     * 被类型匹配器判断通过，然后在下面的这个write方法中被编码，然后用ByteBuf来包装编码后的字节
     * 最后一路传递到出站的头节点，把消息发送出去。整个流程就是这样，大家要梳理清楚。虽然我们是渐进式学习代码
     * 总是局部地学习一块块知识，但是整体的大局观和流程要时刻梳理，这样才有助于你的工作
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = null;
        try {
            //首先仍然是判断当前传进来的msg是否符合泛型的类型
            //会通过类型匹配器来判断
            if (acceptOutboundMessage(msg)) {
                //转换成对应的类型
                @SuppressWarnings("unchecked")
                I cast = (I) msg;
                //把ByteBuf创建出来，因为最后编码完成的字节还是要放到ByteBuf中的
                buf = allocateBuffer(ctx, cast, preferDirect);
                try {
                    //这里是真正编码的逻辑，是个抽象方法，一般由用户自己实现，这就意味着用户自定义的编码器类
                    //要继承这个MessageToByteEncoder类的
                    //当然，用户也可以使用Netty为大家定义好的编解码器，这里我就不讲解了，大家可以自己查查
                    //其实最核心的就是这个encode抽象方法的实现逻辑
                    encode(ctx, cast, buf);
                } finally {
                    //这时候就要释放cast了，也就是源码的msg的内存。注意，这个cast已经转换成对应的泛型类型了
                    //它不一定是ByteBuf，或者说，它其实就是一个非ByteBuf的对象，因为这些消息的载体对象都是用户自定义的
                    //所以下面并不会把它当作ByteBuf来释放内存空间，而是直接返回false，由垃圾回收机制来释放内存
                    ReferenceCountUtil.release(cast);
                }
                //因为已经编码完毕了，所以判断ByteBuf中是否有数据了
                if (buf.isReadable()) {
                    //有数据就在管道上一路传递即可，最后出站发送出去
                    ctx.write(buf, promise);
                } else {
                    //没有数据就释放ByteBuf
                    buf.release();
                    //传递一个空的ByteBuf给下一个处理器
                    //这行被注释掉的代码是源码，因为没有引入Unpooled，所以就注释掉了
                    //ctx.write(Unpooled.EMPTY_BUFFER, promise);
                    ctx.write(null, promise);
                }
                buf = null;
            } else {
                //到这里就说明一开始类型就不匹配，直接向下一个handler传递即可
                ctx.write(msg, promise);
            }
        } catch (EncoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new EncoderException(e);
        } finally {
            if (buf != null) {
                buf.release();
            }
        }
    }


    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, @SuppressWarnings("unused") I msg,
                                     boolean preferDirect) throws Exception {
        if (preferDirect) {
            //这里没有传入一个要分配的内存容量，是因为在内部用一个初始值256分配内存了
            //当然，也可能会遇到分配的内存不够的情况，这时候ByteBuf会自动扩容的
            //大家可以顺着逻辑点一点，方法在AbstractByteBuf类中
            return ctx.alloc().ioBuffer();
        } else {
            return ctx.alloc().heapBuffer();
        }
    }

    /**
     * @Author: PP-jessica
     * @Description:编码的抽象方法，要交给子类来实现
     */
    protected abstract void encode(ChannelHandlerContext ctx, I msg, ByteBuf out) throws Exception;

    protected boolean isPreferDirect() {
        return preferDirect;
    }
}
