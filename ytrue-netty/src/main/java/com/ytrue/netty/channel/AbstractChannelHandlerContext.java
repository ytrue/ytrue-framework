package com.ytrue.netty.channel;

import com.ytrue.netty.util.Attribute;
import com.ytrue.netty.util.AttributeKey;
import com.ytrue.netty.util.ResourceLeakHint;
import com.ytrue.netty.util.concurrent.EventExecutor;
import com.ytrue.netty.util.internal.ObjectUtil;
import com.ytrue.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static com.ytrue.netty.channel.ChannelHandlerMask.*;

/**
 * @author ytrue
 * @date 2023/7/29 13:45
 * @description AbstractChannelHandlerContext
 */
@Slf4j
public abstract class AbstractChannelHandlerContext implements ChannelHandlerContext, ResourceLeakHint {

    /**
     * 下一个节点
     */
    volatile AbstractChannelHandlerContext next;

    /**
     * 上一个节点
     */
    volatile AbstractChannelHandlerContext prev;

    /**
     * ChannelHandler的状态原子更新器
     */
    private static final AtomicIntegerFieldUpdater<AbstractChannelHandlerContext> HANDLER_STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AbstractChannelHandlerContext.class, "handlerState");

    /**
     * 之前讲到的ChannelHandler添加到ChannelPipeline链表中时，会有一个添加状态，只有状态为ADD_COMPLETE的handler才能处理数据
     */
    private static final int ADD_PENDING = 1;
    private static final int ADD_COMPLETE = 2;
    private static final int REMOVE_COMPLETE = 3;

    /**
     * ChannelHandler添加链表后的初始状态
     */
    private static final int INIT = 0;

    /**
     * ChannelPipeline可以得到每一个ChannelHandler，而每一个封装着ChannelHandler的ChannelHandlerContext又可以得到
     */
    private final DefaultChannelPipeline pipeline;

    /**
     * ChannelHandler所对应的名字
     */
    private final String name;

    /**
     * 该值为false，ChannelHandler状态为ADD_PENDING的时候，也可以响应pipeline中的事件
     * 该值为true表示只有ChannelHandler的状态为ADD_COMPLETE时，才能响应pipeline中的事件
     */
    private final boolean ordered;

    /**
     * 这是个很有意思的属性，变量名称为执行掩码，看名字肯定一头雾水，用起来却很有意思。
     * 试想一下，也许我们会向ChannelPipeline中添加很多handler，每个handler都有channelRead，如果有的handler并不对read事件感兴趣，
     * 数据在链表中传递的时候，就会自动跳过该handler。这个掩码，就是表明该handler对哪个事件感兴趣的
     */
    private final int executionMask;

    /**
     * 执行器
     */
    final EventExecutor executor;

    /**
     * 成功的ChannelFuture
     */
    private ChannelFuture succeededFuture;

    /**
     * 把初始状态赋值给handlerState，handlerState属性就是ChannelHandler刚添加到链表时的状态
     */
    private volatile int handlerState = INIT;


    /**
     * 构造
     *
     * @param pipeline     对于的pipeline
     * @param executor     执行器
     * @param name         名字
     * @param handlerClass 对于的类
     */
    AbstractChannelHandlerContext(DefaultChannelPipeline pipeline, EventExecutor executor, String name, Class<? extends ChannelHandler> handlerClass) {
        this.name = ObjectUtil.checkNotNull(name, "name");
        this.pipeline = pipeline;
        this.executor = executor;
        //channelHandlerContext中保存channelHandler的执行条件掩码（是什么类型的ChannelHandler,对什么事件感兴趣）
        this.executionMask = mask(handlerClass);
        ordered = executor == null;
    }

    @Override
    public Channel channel() {
        return pipeline.channel();
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }


    // 该方法暂时不引入，到ByteBuf时再引入
    //    @Override
//    public ByteBufAllocator alloc() {
//        return channel().config().getAllocator();
//    }


    @Override
    public EventExecutor executor() {
        if (executor == null) {
            return channel().eventLoop();
        } else {
            return executor;
        }
    }


    @Override
    public String name() {
        return name;
    }


    @Override
    public ChannelHandlerContext fireChannelRegistered() {
        invokeChannelRegistered(findContextInbound(MASK_CHANNEL_REGISTERED));
        return this;
    }

    /**
     * 执行该handler中的ChannelRegistered方法，从该方法可以看出，一旦channel绑定了单线程执行器，
     * 那么关于该channel的一切，都要由单线程执行器来执行和处理。如果当前调用方法的线程不是单线程执行器的线程，那就
     * 把要进行的动作封装为异步任务提交给执行器
     *
     * @param next
     */
    static void invokeChannelRegistered(AbstractChannelHandlerContext next) {
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeChannelRegistered();
        } else {
            executor.execute(() -> next.invokeChannelRegistered());
        }
    }

    /**
     * 真正执行handler中的ChannelRegistered方法
     */
    private void invokeChannelRegistered() {
        // 接下来会一直看见invokeHandler这个方法，这个方法就是判断CannelHandler在链表中的状态，只有是ADD_COMPLETE，
        // 才会返回true，方法才能继续向下运行，如果返回false，那就进入else分支，会跳过该节点，寻找下一个可以处理数据的节点
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelRegistered(this);
            } catch (Throwable t) {
                notifyHandlerException(t);
            }
        } else {
            fireChannelRegistered();
        }
    }


    @Override
    public ChannelHandlerContext fireChannelUnregistered() {
        invokeChannelUnregistered(findContextInbound(MASK_CHANNEL_UNREGISTERED));
        return this;
    }

    static void invokeChannelUnregistered(AbstractChannelHandlerContext next) {
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeChannelUnregistered();
        } else {
            executor.execute(() -> next.invokeChannelUnregistered());
        }
    }

    /**
     * 真正执行handler中的ChannelUnregistered方法
     */
    private void invokeChannelUnregistered() {
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelUnregistered(this);
            } catch (Throwable t) {
                notifyHandlerException(t);
            }
        } else {
            fireChannelUnregistered();
        }
    }


    @Override
    public ChannelHandlerContext fireChannelActive() {
        invokeChannelActive(findContextInbound(MASK_CHANNEL_ACTIVE));
        return this;
    }

    static void invokeChannelActive(AbstractChannelHandlerContext next) {
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeChannelActive();
        } else {
            executor.execute(() -> next.invokeChannelActive());
        }
    }

    private void invokeChannelActive() {
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelActive(this);
            } catch (Throwable t) {
                notifyHandlerException(t);
            }
        } else {
            fireChannelActive();
        }
    }

    @Override
    public ChannelHandlerContext fireChannelInactive() {
        invokeChannelInactive(findContextInbound(MASK_CHANNEL_INACTIVE));
        return this;
    }

    static void invokeChannelInactive(final AbstractChannelHandlerContext next) {
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeChannelInactive();
        } else {
            executor.execute(() -> next.invokeChannelInactive());
        }
    }

    private void invokeChannelInactive() {
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelInactive(this);
            } catch (Throwable t) {
                notifyHandlerException(t);
            }
        } else {
            fireChannelInactive();
        }
    }

    @Override
    public ChannelHandlerContext fireExceptionCaught(final Throwable cause) {
        invokeExceptionCaught(findContextInbound(MASK_EXCEPTION_CAUGHT), cause);
        return this;
    }

    static void invokeExceptionCaught(final AbstractChannelHandlerContext next, final Throwable cause) {
        ObjectUtil.checkNotNull(cause, "cause");
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeExceptionCaught(cause);
        } else {
            try {
                executor.execute(() -> next.invokeExceptionCaught(cause));
            } catch (Throwable t) {
                if (log.isWarnEnabled()) {
                    log.warn("Failed to submit an exceptionCaught() event.", t);
                    log.warn("The exceptionCaught() event that was failed to submit was:", cause);
                }
            }
        }
    }

    @Override
    public ChannelHandlerContext fireUserEventTriggered(final Object event) {
        invokeUserEventTriggered(findContextInbound(MASK_USER_EVENT_TRIGGERED), event);
        return this;
    }

    static void invokeUserEventTriggered(final AbstractChannelHandlerContext next, final Object event) {
        ObjectUtil.checkNotNull(event, "event");
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeUserEventTriggered(event);
        } else {
            executor.execute(() -> next.invokeUserEventTriggered(event));
        }
    }

    private void invokeUserEventTriggered(Object event) {
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).userEventTriggered(this, event);
            } catch (Throwable t) {
                notifyHandlerException(t);
            }
        } else {
            fireUserEventTriggered(event);
        }
    }

    @Override
    public ChannelHandlerContext fireChannelRead(final Object msg) {
        invokeChannelRead(findContextInbound(MASK_CHANNEL_READ), msg);
        return this;
    }

    static void invokeChannelRead(final AbstractChannelHandlerContext next, Object msg) {
        //final Object m = next.pipeline.touch(ObjectUtil.checkNotNull(msg, "msg"), next);
        final Object m = msg;
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeChannelRead(m);
        } else {
            executor.execute(() -> next.invokeChannelRead(m));
        }
    }

    private void invokeChannelRead(Object msg) {
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelRead(this, msg);
            } catch (Throwable t) {
                notifyHandlerException(t);
            }
        } else {
            fireChannelRead(msg);
        }
    }

    @Override
    public ChannelHandlerContext fireChannelReadComplete() {
        invokeChannelReadComplete(findContextInbound(MASK_CHANNEL_READ_COMPLETE));
        return this;
    }

    static void invokeChannelReadComplete(final AbstractChannelHandlerContext next) {
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeChannelReadComplete();
        }
//        一般来说是不会走到下面这个分支的,所以先注释了,不必再引入更多的类
//        else {
//            Tasks tasks = next.invokeTasks;
//            if (tasks == null) {
//                next.invokeTasks = tasks = new Tasks(next);
//            }
//            executor.execute(tasks.invokeChannelReadCompleteTask);
//        }
    }

    private void invokeChannelReadComplete() {
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelReadComplete(this);
            } catch (Throwable t) {
                notifyHandlerException(t);
            }
        } else {
            fireChannelReadComplete();
        }
    }


    @Override
    public ChannelHandlerContext fireChannelWritabilityChanged() {
        invokeChannelWritabilityChanged(findContextInbound(MASK_CHANNEL_WRITABILITY_CHANGED));
        return this;
    }

    static void invokeChannelWritabilityChanged(final AbstractChannelHandlerContext next) {
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeChannelWritabilityChanged();
        }
//        else {
//            Tasks tasks = next.invokeTasks;
//            if (tasks == null) {
//                next.invokeTasks = tasks = new Tasks(next);
//            }
//            executor.execute(tasks.invokeChannelWritableStateChangedTask);
//        }
    }

    private void invokeChannelWritabilityChanged() {
        if (invokeHandler()) {
            try {
                ((ChannelInboundHandler) handler()).channelWritabilityChanged(this);
            } catch (Throwable t) {
                notifyHandlerException(t);
            }
        } else {
            fireChannelWritabilityChanged();
        }
    }


    /**
     * 这几个方法应该很熟悉了吧，都是出站处理器中的方法，这时候大家也应该明白了，
     * 每当我调用一个方法时，比如说就是服务端channel的绑定端口号的bind方法，调用链路会先从AbstractChannel类中开始，
     * 但是，channel拥有ChannelPipeline链表，链表中有一系列的处理器，所以调用链就会跑到ChannelPipeline中，然后从ChannelPipeline
     * 又跑到每一个ChannelHandler中，经过这些ChannelHandler的处理，调用链又会跑到channel的内部类Unsafe中，再经过一系列的调用，
     * 最后来到NioServerSocketChannel中，执行真正的doBind方法。
     *
     * @return
     */

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return bind(localAddress, newPromise());
    }

    @Override
    public ChannelFuture bind(final SocketAddress localAddress, final ChannelPromise promise) {
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }
        //找到对bind事件感兴趣的handler
        final AbstractChannelHandlerContext next = findContextOutbound(MASK_BIND);
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            //这个时候肯定是单线程执行器接管了channel，所以会走这个分支
            next.invokeBind(localAddress, promise);
        } else {
            safeExecute(executor, () -> next.invokeBind(localAddress, promise), promise, null);
        }
        return promise;
    }

    private void invokeBind(SocketAddress localAddress, ChannelPromise promise) {
        if (invokeHandler()) {
            try {
                // 每次都要调用handler()方法来获得handler，但是接口中的handler方法是在哪里实现的呢？
                // 在DefaultChannelHandlerContext类中，这也提醒着我们，我们创建的context节点是DefaultChannelHandlerContext节点。
                ((ChannelOutboundHandler) handler()).bind(this, localAddress, promise);
            } catch (Throwable t) {
                notifyOutboundHandlerException(t, promise);
            }
        } else {
            bind(localAddress, promise);
        }
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return connect(remoteAddress, newPromise());
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return connect(remoteAddress, localAddress, newPromise());
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return connect(remoteAddress, null, promise);
    }

    @Override
    public ChannelFuture connect(
            final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {

        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        }
        final AbstractChannelHandlerContext next = findContextOutbound(MASK_CONNECT);
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeConnect(remoteAddress, localAddress, promise);
        } else {
            safeExecute(executor, () -> next.invokeConnect(remoteAddress, localAddress, promise), promise, null);
        }
        return promise;
    }

    private void invokeConnect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        if (invokeHandler()) {
            try {
                ((ChannelOutboundHandler) handler()).connect(this, remoteAddress, localAddress, promise);
            } catch (Throwable t) {
                notifyOutboundHandlerException(t, promise);
            }
        } else {
            connect(remoteAddress, localAddress, promise);
        }
    }

    @Override
    public ChannelFuture disconnect() {
        return disconnect(newPromise());
    }

    @Override
    public ChannelFuture disconnect(final ChannelPromise promise) {
        final AbstractChannelHandlerContext next = findContextOutbound(MASK_DISCONNECT);
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeDisconnect(promise);
        } else {
            safeExecute(executor, () -> next.invokeDisconnect(promise), promise, null);
        }
        return promise;
    }

    private void invokeDisconnect(ChannelPromise promise) {
        if (invokeHandler()) {
            try {
                ((ChannelOutboundHandler) handler()).disconnect(this, promise);
            } catch (Throwable t) {
                notifyOutboundHandlerException(t, promise);
            }
        } else {
            disconnect(promise);
        }
    }

    @Override
    public ChannelFuture close() {
        return close(newPromise());
    }

    @Override
    public ChannelFuture close(final ChannelPromise promise) {
        final AbstractChannelHandlerContext next = findContextOutbound(MASK_CLOSE);
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeClose(promise);
        } else {
            safeExecute(executor, new Runnable() {
                @Override
                public void run() {
                    next.invokeClose(promise);
                }
            }, promise, null);
        }

        return promise;
    }

    private void invokeClose(ChannelPromise promise) {
        if (invokeHandler()) {
            try {
                ((ChannelOutboundHandler) handler()).close(this, promise);
            } catch (Throwable t) {
                notifyOutboundHandlerException(t, promise);
            }
        } else {
            close(promise);
        }
    }


    @Override
    public ChannelFuture deregister() {
        return deregister(newPromise());
    }

    @Override
    public ChannelFuture deregister(final ChannelPromise promise) {
        if (isNotValidPromise(promise, false)) {
            return promise;
        }
        final AbstractChannelHandlerContext next = findContextOutbound(MASK_DEREGISTER);
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeDeregister(promise);
        } else {
            safeExecute(executor, () -> next.invokeDeregister(promise), promise, null);
        }

        return promise;
    }

    private void invokeDeregister(ChannelPromise promise) {
        if (invokeHandler()) {
            try {
                ((ChannelOutboundHandler) handler()).deregister(this, promise);
            } catch (Throwable t) {
                notifyOutboundHandlerException(t, promise);
            }
        } else {
            deregister(promise);
        }
    }

    @Override
    public ChannelHandlerContext read() {
        final AbstractChannelHandlerContext next = findContextOutbound(MASK_READ);
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeRead();
        }
//        else {
//            Tasks tasks = next.invokeTasks;
//            if (tasks == null) {
//                next.invokeTasks = tasks = new Tasks(next);
//            }
//            executor.execute(tasks.invokeReadTask);
//        }

        return this;
    }

    private void invokeRead() {
        if (invokeHandler()) {
            try {
                ((ChannelOutboundHandler) handler()).read(this);
            } catch (Throwable t) {
                notifyHandlerException(t);
            }
        } else {
            read();
        }
    }

    @Override
    public ChannelFuture write(Object msg) {
        return write(msg, newPromise());
    }

    @Override
    public ChannelFuture write(final Object msg, final ChannelPromise promise) {
        write(msg, false, promise);

        return promise;
    }


    private void write(Object msg, boolean flush, ChannelPromise promise) {
        ObjectUtil.checkNotNull(msg, "msg");
        final AbstractChannelHandlerContext next = findContextOutbound(flush ?
                (MASK_WRITE | MASK_FLUSH) : MASK_WRITE);
        final Object m = msg;
        //该方法用来检查内存是否泄漏，因为还未引入，所以暂时注释掉
        //final Object m = pipeline.touch(msg, next);
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            if (flush) {
                next.invokeWriteAndFlush(m, promise);
            } else {
                next.invokeWrite(m, promise);
            }
        } else {
            //下面被注释掉的分支是源码，我们用的这个else分支是我自己写的，等真正讲到WriteAndFlush方法时，我们再讲解源码
            executor.execute(() -> next.invokeWriteAndFlush(m, promise));
        }
//        else {
//            final AbstractWriteTask task;
//            if (flush) {
//                task = WriteAndFlushTask.newInstance(next, m, promise);
//            }  else {
//                task = WriteTask.newInstance(next, m, promise);
//            }
//            if (!safeExecute(executor, task, promise, m)) {
//                task.cancel();
//            }
//        }
    }

    private void invokeWrite(Object msg, ChannelPromise promise) {
        if (invokeHandler()) {
            invokeWrite0(msg, promise);
        } else {
            write(msg, promise);
        }
    }

    private void invokeWrite0(Object msg, ChannelPromise promise) {
        try {
            ((ChannelOutboundHandler) handler()).write(this, msg, promise);
        } catch (Throwable t) {
            notifyOutboundHandlerException(t, promise);
        }
    }

    @Override
    public ChannelHandlerContext flush() {
        final AbstractChannelHandlerContext next = findContextOutbound(MASK_FLUSH);
        EventExecutor executor = next.executor();
        if (executor.inEventLoop(Thread.currentThread())) {
            next.invokeFlush();
        }
//        else {
//            Tasks tasks = next.invokeTasks;
//            if (tasks == null) {
//                next.invokeTasks = tasks = new Tasks(next);
//            }
//            safeExecute(executor, tasks.invokeFlushTask, channel().voidPromise(), null);
//        }
        return this;
    }

    private void invokeFlush() {
        if (invokeHandler()) {
            //发送缓冲区的数据
            invokeFlush0();
        } else {
            flush();
        }
    }

    private void invokeFlush0() {
        try {
            ((ChannelOutboundHandler) handler()).flush(this);
        } catch (Throwable t) {
            notifyHandlerException(t);
        }
    }


    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return writeAndFlush(msg, newPromise());
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        write(msg, true, promise);
        return promise;
    }

    private void invokeWriteAndFlush(Object msg, ChannelPromise promise) {
        if (invokeHandler()) {
            invokeWrite0(msg, promise);
            invokeFlush0();
        } else {
            writeAndFlush(msg, promise);
        }
    }

    @Override
    public ChannelPromise newPromise() {
        return new DefaultChannelPromise(channel(), executor());
    }

    /**
     * 该方法做了一点小改动，我没有引入SucceededChannelFuture类，不是核心方法，看看就行
     *
     * @return
     */
    @Override
    public ChannelFuture newSucceededFuture() {
        ChannelFuture succeededFuture = this.succeededFuture;
        if (succeededFuture == null) {
            this.succeededFuture = succeededFuture = new DefaultChannelPromise(channel(), executor());
        }
        return succeededFuture;
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        //return new FailedChannelFuture(channel(), executor(), cause);
        return null;
    }


    private static boolean safeExecute(EventExecutor executor, Runnable runnable, ChannelPromise promise, Object msg) {
        try {
            executor.execute(runnable);
            return true;
        } catch (Throwable cause) {
            try {
                promise.setFailure(cause);
            } finally {
                if (msg != null) {
                    //当该引用计数减至为0时，该ByteBuf即可回收，我们还未讲到这里，所以我先注释掉这个方法
                    //ReferenceCountUtil.release(msg);
                }
            }
            return false;
        }
    }


    private static void notifyOutboundHandlerException(Throwable cause, ChannelPromise promise) {
        //PromiseNotificationUtil.tryFailure(promise, cause, promise instanceof VoidChannelPromise ? null : logger);
    }


    private boolean isNotValidPromise(ChannelPromise promise, boolean allowVoidPromise) {
        if (promise == null) {
            throw new NullPointerException("promise");
        }
        if (promise.isDone()) {
            if (promise.isCancelled()) {
                return true;
            }
            throw new IllegalArgumentException("promise already done: " + promise);
        }
        if (promise.channel() != channel()) {
            throw new IllegalArgumentException(String.format(
                    "promise.channel does not match: %s (expected: %s)", promise.channel(), channel()));
        }
        if (promise.getClass() == DefaultChannelPromise.class) {
            return false;
        }
        if (promise instanceof AbstractChannel.CloseFuture) {
            throw new IllegalArgumentException(
                    StringUtil.simpleClassName(AbstractChannel.CloseFuture.class) + " not allowed in a pipeline");
        }
        return false;
    }


    /**
     * 调用异常处理器
     *
     * @param cause
     */
    private void notifyHandlerException(Throwable cause) {
        if (inExceptionCaught(cause)) {
            if (log.isWarnEnabled()) {
                log.warn("An exception was thrown by a user handler " + "while handling an exceptionCaught event", cause);
            }
            return;
        }

        invokeExceptionCaught(cause);
    }

    /**
     * 调异常处理器
     *
     * @param cause
     */
    private void invokeExceptionCaught(Throwable cause) {
        if (invokeHandler()) {
            try {
                handler().exceptionCaught(this, cause);
            } catch (Throwable error) {
                if (log.isDebugEnabled()) {
                    log.debug("An exception {}" + "was thrown by a user handler's exceptionCaught() " + "method while handling the following exception:",
                            //ThrowableUtil.stackTraceToString(error),
                            cause);
                } else if (log.isWarnEnabled()) {
                    log.warn("An exception '{}' [enable DEBUG level for full stacktrace] " + "was thrown by a user handler's exceptionCaught() " + "method while handling the following exception:", error, cause);
                }
            }
        } else {
            fireExceptionCaught(cause);
        }
    }

    /**
     * 用于判断是否在异常处理程序（exceptionCaught）中捕获了异常。
     *
     * @param cause
     * @return
     */
    private static boolean inExceptionCaught(Throwable cause) {
        //- 首先，将传入的异常对象赋值给变量cause。
        //- 然后，循环遍历异常对象及其所有的cause，直到找到异常堆栈中包含“exceptionCaught”方法名的StackTraceElement对象或者cause为null为止。
        //- 在每次循环中，通过调用cause.getStackTrace()获取异常堆栈信息，并遍历所有的StackTraceElement对象。
        //- 如果找到了一个StackTraceElement对象的方法名为“exceptionCaught”，则返回true，表示异常已经被捕获。
        //- 如果在所有的异常堆栈中都没有找到“exceptionCaught”方法名的StackTraceElement对象，则返回false，表示异常未被捕获。
        // 总结来说，该方法的作用是判断是否在异常处理程序中捕获了异常。它通过遍历异常对象及其所有的cause，查找是否存在“exceptionCaught”方法名的StackTraceElement对象来实现。
        // 该方法在Netty的异常处理过程中起到了关键的作用，用于判断异常是否已经被捕获，并进行相应的处理。
        do {
            StackTraceElement[] trace = cause.getStackTrace();
            if (trace != null) {
                for (StackTraceElement t : trace) {
                    if (t == null) {
                        break;
                    }
                    if ("exceptionCaught".equals(t.getMethodName())) {
                        return true;
                    }
                }
            }

            cause = cause.getCause();
        } while (cause != null);
        return false;
    }

    /**
     * 判断ChannelPipeline中节点的状态是否为ADD_COMPLETE，只有状态为ADD_COMPLETE时，handler才可以处理数据
     *
     * @return
     */
    private boolean invokeHandler() {
        int handlerState = this.handlerState;
        return handlerState == ADD_COMPLETE || (!ordered && handlerState == ADD_PENDING);
    }


    /**
     * 该方法的作用是在ChannelPipeline中查找具有指定入站事件掩码的AbstractChannelHandlerContext对象，
     * 并返回找到的对象。
     * 这个方法在Netty的事件传播过程中起到了关键的作用，用于确定事件的传递路径和处理器的执行顺序。
     *
     * @param mask
     * @return
     */
    private AbstractChannelHandlerContext findContextInbound(int mask) {
        //  首先，将当前的AbstractChannelHandlerContext赋值给变量ctx。
        //- 然后，循环遍历Pipeline中的下一个AbstractChannelHandlerContext，直到找到具有指定入站事件掩码的AbstractChannelHandlerContext为止。
        //- 在每次循环中，通过将ctx的next属性赋值给ctx，将ctx移动到下一个AbstractChannelHandlerContext。
        //- 循环继续的条件是，ctx的executionMask与指定的入站事件掩码进行按位与运算的结果为0，即ctx的executionMask不包含指定的入站事件掩码。
        //- 当找到具有指定入站事件掩码的AbstractChannelHandlerContext时，返回该AbstractChannelHandlerContext对象。

        AbstractChannelHandlerContext ctx = this;
        do {
            //为什么获取后一个？因为是入站处理器，数据从前往后传输
            ctx = ctx.next;
        } while ((ctx.executionMask & mask) == 0);
        return ctx;
    }

    private AbstractChannelHandlerContext findContextOutbound(int mask) {
        AbstractChannelHandlerContext ctx = this;
        do {
            //为什么获取前一个？因为是出站处理器，数据从后往前传输
            ctx = ctx.prev;
            //做&运算，判断事件合集中是否包含该事件
        } while ((ctx.executionMask & mask) == 0);
        return ctx;
    }


    /**
     * 把链表中的ChannelHandler的状态设置为删除完成
     */
    final void setRemoved() {
        handlerState = REMOVE_COMPLETE;
    }

    /**
     * 把链表中的ChannelHandler的状态设置为添加完成
     *
     * @return
     */
    final boolean setAddComplete() {
        for (; ; ) {
            int oldState = handlerState;
            if (oldState == REMOVE_COMPLETE) {
                return false;
            }
            if (HANDLER_STATE_UPDATER.compareAndSet(this, oldState, ADD_COMPLETE)) {
                return true;
            }
        }
    }

    /**
     * 把链表中的ChannelHandler的状态设置为等待添加
     */
    final void setAddPending() {
        boolean updated = HANDLER_STATE_UPDATER.compareAndSet(this, INIT, ADD_PENDING);
        assert updated;
    }


    /**
     * 在该方法中，ChannelHandler的添加状态将变为添加完成，然后ChannelHandler调用它的handlerAdded方法
     *
     * @throws Exception
     */
    final void callHandlerAdded() throws Exception {
        //在这里改变channelhandler的状态
        if (setAddComplete()) {
            handler().handlerAdded(this);
        }
    }

    /**
     * :回调链表中节点的handlerRemoved方法，该方法在ChannelPipeline中有节点被删除时被调用。
     *
     * @throws Exception
     */
    final void callHandlerRemoved() throws Exception {
        try {
            if (handlerState == ADD_COMPLETE) {
                handler().handlerRemoved(this);
            }
        } finally {
            setRemoved();
        }
    }

    @Override
    public boolean isRemoved() {
        return handlerState == REMOVE_COMPLETE;
    }

    /**
     * 该方法就可以得到用户存储在channel这个map中的数据，每一个handler都可以得到
     *
     * @param key
     * @param <T>
     * @return
     */
    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return channel().attr(key);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return channel().hasAttr(key);
    }

    @Override
    public String toHintString() {
        return '\'' + name + "' will handle the message from this point.";
    }

    @Override
    public String toString() {
        return StringUtil.simpleClassName(ChannelHandlerContext.class) + '(' + name + ", " + channel() + ')';
    }

}
