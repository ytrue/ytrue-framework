package com.ytrue.netty.channel.nio;

import com.ytrue.netty.channel.*;
import com.ytrue.netty.util.IntSupplier;
import com.ytrue.netty.util.concurrent.RejectedExecutionHandler;
import com.ytrue.netty.util.internal.PlatformDependent;
import com.ytrue.netty.util.internal.ReflectionUtil;
import com.ytrue.netty.util.internal.SystemPropertyUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ytrue
 * @date 2023-07-22 14:23
 * @description NioEventLoop
 */
/**
 * @Author: ytrue
 * @Description:重构之后的NioEventLoop类。大家仔细看看，新添加了很多属性和方法
 */
public class NioEventLoop extends SingleThreadEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(NioEventLoop.class);

    private static final int CLEANUP_INTERVAL = 256;
    /**
     * @Author: ytrue
     * @Description:现在，这个属性就成了被包装过后的selector了，下面这个属性，就是原生的selector
     * 但是，我们注册channel到多路复用器，使用的仍然是原生的selector，也就是下面那个，虽然其他时候使用的
     * 的是被包装的selector，但是它内部其实持有了原生selector，最终也会调用原生selector来执行任务的
     */
    private Selector selector;
    private Selector unwrappedSelector;

    //已就绪的io事件集合，这个是Netty自己定义的，这个要通过反射设置到selector中，取代selector中的selectedKeys
    /**
     * @Author: ytrue
     * @Description:这个成员变量就是netty自己定义的，具体解释可以在SelectedSelectionKeySet类的注释中找到
     */
    private SelectedSelectionKeySet selectedKeys;

    private final SelectorProvider provider;

    /**
     * @Author: ytrue
     * @Description:原子类，这个成员变量的作用是用来表明当前selector是否正在select方法阻塞着
     */
    private final AtomicBoolean wakenUp = new AtomicBoolean();
    /**
     * @Author: ytrue
     * @Description:一个选择器，在run方法中会看见具体用法
     */
    private  SelectStrategy selectStrategy;

    /**
     * @Author: ytrue
     * @Description:单线程执行器执行IO事件的百分比，默认为百分之50，就是IO事件和用户提交的定时任务各执行一半
     * 总的来说，就是根据这个百分比，各IO事件和用户提交的任务分配单线程执行器的使用时间
     */
    private volatile int ioRatio = 50;
    /**
     * @Author: ytrue
     * @Description:被取消的key的个数
     */
    private int cancelledKeys;
    /**
     * @Author: ytrue
     * @Description:是否需要重新select
     */
    private boolean needsToSelectAgain;

    //这里是禁止使用优化的key，但默认是false，所以是默认使用优化过的key
    /**
     * @Author: ytrue
     * @Description:成员变量的意思是是否禁用优化过的key，也就是SelectedSelectionKeySet属性
     * 这个类的内部是一个数组，遍历会比set快一点，但是请注意，这里默认是false，因此是不禁用优化的key
     * 所以就是使用优化的key
     */
    private static final boolean DISABLE_KEY_SET_OPTIMIZATION =
            SystemPropertyUtil.getBoolean("io.netty.noKeySetOptimization", false);
    /**
     * @Author: ytrue
     * @Description:下面这两个属性作用差不多，都是和重建selector有关
     * 因为nio有空轮训bug，netty为了解决这个问题，就弄了一个阈值出来，就是下面的第二个属性，会被512赋值，这个就是默认的阈值
     * 空轮训次数超过512次，就会重建selector，简单来说就是把之前注册到channel注册到新的selector上，然后把出现问题的selector销毁就行
     * 而下面的第一个属性就是一个用来判断的阈值，如果用户设定的阈值小于3，阈值就会被设置为0
     * 在静态代码块中，会看到这些成员变量被用到了
     */
    private static final int MIN_PREMATURE_SELECTOR_RETURNS = 3;
    private static final int SELECTOR_AUTO_REBUILD_THRESHOLD;

    /**
     * @Author: ytrue
     * @Description:该方法会立即返回已就绪的IO事件的个数，没有则返回0，并不会像select那样阻塞
     * 同样是在该类的run方法中被用到了
     */
    private final IntSupplier selectNowSupplier = new IntSupplier() {
        @Override
        public int get() throws Exception {
            return selectNow();
        }
    };

    /**
     * @Author: ytrue
     * @Description:静态代码块开始给上面的某些成员变量赋值了
     */
    static {
        //这里不用关注
        final String key = "sun.nio.ch.bugLevel";
        final String bugLevel = SystemPropertyUtil.get(key);
        if (bugLevel == null) {
            try {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        System.setProperty(key, "");
                        return null;
                    }
                });
            } catch (final SecurityException e) {
                logger.debug("Unable to get/set System Property: " + key, e);
            }
        }
        //这里就是给上面的重建selector的阈值赋值了
        //这里先判断有没有给SELECTOR_AUTO_REBUILD_THRESHOLD成员变量赋值，先得到该成员变量的值，默认为512
        int selectorAutoRebuildThreshold = SystemPropertyUtil.getInt("io.netty.selectorAutoRebuildThreshold", 512);
        //如果不为512，就说明用户可能自己设定值了，
        //判断该值是不是小于3，如果小于3，那阈值就会被赋值为0
        if (selectorAutoRebuildThreshold < MIN_PREMATURE_SELECTOR_RETURNS) {
            selectorAutoRebuildThreshold = 0;
        }
        //在这里被真正赋值，不是默认值就是用户设定的值，如果设定的值小于3，阈值就会被设置为0
        SELECTOR_AUTO_REBUILD_THRESHOLD = selectorAutoRebuildThreshold;
        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.noKeySetOptimization: {}", DISABLE_KEY_SET_OPTIMIZATION);
            logger.debug("-Dio.netty.selectorAutoRebuildThreshold: {}", SELECTOR_AUTO_REBUILD_THRESHOLD);
        }
    }

    /**
     * @Author: ytrue
     * @Description:构造函数，就不再详细注释了，都是很简单的东西
     */
    NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider,
                 SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler,
                 EventLoopTaskQueueFactory queueFactory) {
        super(parent, executor, false, newTaskQueue(queueFactory), newTaskQueue(queueFactory),
                rejectedExecutionHandler);
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        if (strategy == null) {
            throw new NullPointerException("selectStrategy");
        }
        provider = selectorProvider;
        //openSelector()方法就是包装原生selecotr的
        final SelectorTuple selectorTuple = openSelector();
        //把包装过的selector赋值
        selector = selectorTuple.selector;
        //同时也给原生selector赋值
        unwrappedSelector = selectorTuple.unwrappedSelector;
        selectStrategy = strategy;
    }

    /**
     * @Author: ytrue
     * @Description:这里其实就用到了无锁的高性能队列，这些队列要传递到单线程执行器的类中，所以大家应该也意识到了
     * 其实单线程执行器中，使用的就是高性能无锁队列
     * 当然，有个前提就是判断queueFactory是不是null
     */
    private static Queue<Runnable> newTaskQueue(EventLoopTaskQueueFactory queueFactory) {
        if (queueFactory == null) {
            return newTaskQueue0(DEFAULT_MAX_PENDING_TASKS);
        }
        return queueFactory.newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
    }

    /**
     * @Author: ytrue
     * @Description:一个静态内部类，这个类就是返回被包装过后的selector的
     */
    private static final class SelectorTuple {
        //这个类内部持有了原生的selector，就是下面这个未被包装过的selector
        final Selector unwrappedSelector;
        //下面这个就是被包装过的selector
        final Selector selector;

        //构造器
        SelectorTuple(Selector unwrappedSelector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = unwrappedSelector;
        }
        //使用的是这个构造器
        SelectorTuple(Selector unwrappedSelector, Selector selector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = selector;
        }
    }

    /**
     * @Author: ytrue
     * @Description:selector就是在该方法内被包装的，非常重要
     */
    private SelectorTuple openSelector() {
        final Selector unwrappedSelector;
        try {
            //这里通过provider得到了nio原生的sleector
            unwrappedSelector = provider.openSelector();
        } catch (IOException e) {
            throw new ChannelException("failed to open a new selector", e);
        }
        //这个默认值为false，还记得这个属性吧，默认不禁用优化key，所以不会走下面的分支
        if (DISABLE_KEY_SET_OPTIMIZATION) {
            return new SelectorTuple(unwrappedSelector);
        }
        //开始使用反射功能了
        Object maybeSelectorImplClass = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    //得到原生selector的实现类
                    //private Set<SelectionKey> publicKeys
                    //private Set<SelectionKey> publicSelectedKeys
                    //上面两个关键属性都在SelectorImpl类中
                    return Class.forName(
                            "sun.nio.ch.SelectorImpl",
                            false,
                            PlatformDependent.getSystemClassLoader());
                } catch (Throwable cause) {
                    return cause;
                }
            }
        });

        //判断得到的对象是不是Class类型，因为反射得到的都是Class呀。
        //这里做了什么呢？maybeSelectorImplClass instanceof Class先判断类型
        //((Class<?>) maybeSelectorImplClass).isAssignableFrom(unwrappedSelector.getClass()))判断是不是属于selector
        //注意，这里的两个判断条件都取反了，也就是说，如果返回的都是true，取反都为false，就不会进入下面的分支
        if (!(maybeSelectorImplClass instanceof Class) ||
            !((Class<?>) maybeSelectorImplClass).isAssignableFrom(unwrappedSelector.getClass())) {
            if (maybeSelectorImplClass instanceof Throwable) {
                Throwable t = (Throwable) maybeSelectorImplClass;
                logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, t);
            }
            return new SelectorTuple(unwrappedSelector);
        }

        //强转一下，得到selectorImplClass
        final Class<?> selectorImplClass = (Class<?>) maybeSelectorImplClass;

        //把SelectedSelectionKeySet对象创建好，因为这个对象要使用反射设置到selector中
        final SelectedSelectionKeySet selectedKeySet = new SelectedSelectionKeySet();

        //下面就是使用反射具体设置属性的操作，用netty自定义的属性取代selector原生的成员变量
        Object maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    //具体的反射操作，这些大家应该都很熟悉吧，我就不再一一注释了
                    Field selectedKeysField = selectorImplClass.getDeclaredField("selectedKeys");
                    Field publicSelectedKeysField = selectorImplClass.getDeclaredField("publicSelectedKeys");

                    //这里是判断jdk的版本，然后用一种手段替换原生属性
                    if (PlatformDependent.javaVersion() >= 9 && PlatformDependent.hasUnsafe()) {
                        long selectedKeysFieldOffset = PlatformDependent.objectFieldOffset(selectedKeysField);
                        long publicSelectedKeysFieldOffset =
                                PlatformDependent.objectFieldOffset(publicSelectedKeysField);
                        if (selectedKeysFieldOffset != -1 && publicSelectedKeysFieldOffset != -1) {
                            PlatformDependent.putObject(
                                    unwrappedSelector, selectedKeysFieldOffset, selectedKeySet);
                            PlatformDependent.putObject(
                                    unwrappedSelector, publicSelectedKeysFieldOffset, selectedKeySet);
                            //注意，这里return了null并不是意味着openSelector方法返回了null，而是这个AccessController.doPrivileged
                            //返回了null，也就是maybeException为null
                            //其实它只要不返回异常都没事，因为我们是使用反射给原生selector替换属性，返回什么并不要紧的
                            //如果替换成功，实际上在这里Java的源码就被修改了，以后使用的就是修改过的selector老人
                            return null;
                        }
                    }

                    Throwable cause = ReflectionUtil.trySetAccessible(selectedKeysField, true);
                    if (cause != null) {
                        return cause;
                    }
                    cause = ReflectionUtil.trySetAccessible(publicSelectedKeysField, true);
                    if (cause != null) {
                        return cause;
                    }
                    //在这里设置成功，unwrappedSelector这个是原生的selector，要反射替换他的属性，所以要把它也传进方法中
                    selectedKeysField.set(unwrappedSelector, selectedKeySet);
                    publicSelectedKeysField.set(unwrappedSelector, selectedKeySet);
                    return null;
                } catch (NoSuchFieldException e) {
                    return e;
                } catch (IllegalAccessException e) {
                    return e;
                }
            }
        });
        //这里就是有异常了
        if (maybeException instanceof Exception) {
            selectedKeys = null;
            Exception e = (Exception) maybeException;
            logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, e);
            return new SelectorTuple(unwrappedSelector);
        }
        selectedKeys = selectedKeySet;
        logger.trace("instrumented a special java.util.Set into: {}", unwrappedSelector);
        //最后在这里，创建了SelectorTuple对象，这时候原生的unwrappedSelector属性已经被替换成功
        //然后创建一个SelectedSelectionKeySetSelector被包装过的selector对象让用户去使用
        return new SelectorTuple(unwrappedSelector, new SelectedSelectionKeySetSelector(unwrappedSelector, selectedKeySet));
    }

    public SelectorProvider selectorProvider() {
        return provider;
    }

    /**
     * @Author: ytrue
     * @Description:返回未包装的selector，因为channel还是要注册到这个selector上的，在doRegister方法中，会用到这个方法
     */
    Selector unwrappedSelector() {
        return unwrappedSelector;
    }


    @Override
    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        return newTaskQueue0(maxPendingTasks);
    }

    private static Queue<Runnable> newTaskQueue0(int maxPendingTasks) {
        return maxPendingTasks == Integer.MAX_VALUE ? PlatformDependent.<Runnable>newMpscQueue()
                : PlatformDependent.<Runnable>newMpscQueue(maxPendingTasks);
    }
    //得到处理IO事件占比
    public int getIoRatio() {
        return ioRatio;
    }

    //设置处理IO事件占比
    public void setIoRatio(int ioRatio) {
        if (ioRatio <= 0 || ioRatio > 100) {
            throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
        }
        this.ioRatio = ioRatio;
    }

    /**
     * @Author: ytrue
     * @Description:重建selector的方法
     */
    public void rebuildSelector() {
        //判断是否为单线程执行器
        if (!inEventLoop(Thread.currentThread())) {
            execute(new Runnable() {
                @Override
                public void run() {
                    //这里是真正执行重构selector的方法
                    rebuildSelector0();
                }
            });
            return;
        }
        //走到这里意味着目前是单线程执行器，直接重构即可
        rebuildSelector0();
    }

    //得到已注册的channel个数
    public int registeredChannels() {
        return selector.keys().size() - cancelledKeys;
    }

    /**
     * @Author: ytrue
     * @Description:真正重构selector的方法
     */
    private void rebuildSelector0() {
        //得到旧的selector，注意，这里得到的是包装过的selector
        final Selector oldSelector = selector;
        //声明一个新的selector
        final SelectorTuple newSelectorTuple;

        //如果旧的为null，直接退出
        if (oldSelector == null) {
            return;
        }

        try {
            //在这里把新的selector创建出来，这里得到的其实还是SelectorTuple对象
            //但这个对象中持有被包装过的selector，流程之前已经看过了
            newSelectorTuple = openSelector();
        } catch (Exception e) {
            logger.warn("Failed to create a new Selector.", e);
            return;
        }

        //下面就要把已经注册的channel全转移到新的selector上
        //移动的channel的个数
        int nChannels = 0;
        //在这里遍历旧的selector
        for (SelectionKey key: oldSelector.keys()) {

            //从附件中得到channel，这里的a实际上就是之前放进附件中的channel
            Object a = key.attachment();

            try {
                //判断key是否有效
                if (!key.isValid() || key.channel().keyFor(newSelectorTuple.unwrappedSelector) != null) {
                    continue;
                }
                //得到感兴趣的事件
                int interestOps = key.interestOps();
                //把原来的key取消
                key.cancel();

                //开始把channel注册到新的selector上了，并且感兴趣的事件，和channel，也就是a，通过附件也放进去
                SelectionKey newKey = key.channel().register(newSelectorTuple.unwrappedSelector, interestOps, a);
                if (a instanceof AbstractNioChannel) {
                    //更新selectionKey，因为上面返回了一个新的key
                    ((AbstractNioChannel) a).selectionKey = newKey;
                }
                //移动的channel数加1
                nChannels ++;
            } catch (Exception e) {
                logger.warn("Failed to re-register a Channel to the new Selector.", e);
                if (a instanceof AbstractNioChannel) {
                    //有一场的话就关闭channel
                    AbstractNioChannel ch = (AbstractNioChannel) a;
                    ch.unsafe().close(ch.unsafe().voidPromise());
                } else {
                    @SuppressWarnings("unchecked")
                    NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                    invokeChannelUnregistered(task, key, e);
                }
            }
        }
        //重新给这两个成员变量赋值
        selector = newSelectorTuple.selector;
        unwrappedSelector = newSelectorTuple.unwrappedSelector;
        try {
            //旧的selector终于可以关闭了
            oldSelector.close();
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to close the old Selector.", t);
            }
        }
        if (logger.isInfoEnabled()) {
            logger.info("Migrated " + nChannels + " channel(s) to the new Selector.");
        }
    }

    /**
     * @Author: ytrue
     * @Description:重构之后的run方法
     */
    @Override
    protected void run() {
        //当然，仍然是在一个死循环中
        for (;;) {
            try {
                try {
                    //这里要判断selectStrategy.calculateStrategy的返回结果。
                    //selectNowSupplier会在方法内部调用它的selectSupplier.get()方法，判断有没有就绪的IO事件
                    //其实就是这一行return hasTasks ? selectSupplier.get() : SelectStrategy.SELECT核心代码
                    //如果现在任务队列中有任务了，然后在判断就绪的IO事件个数，如果没有，就返回0，如果当前任务队列中没有
                    //任务，就直接返回-1，也就是阻塞等待IO事件到来的意思

                    //这里SelectStrategy.CONTINUE，SelectStrategy.BUSY_WAIT，SelectStrategy.SELECT这几个都是负数
                    //所以如果任务队列中有任务，就要去执行任务了，不会再阻塞
                    //然后看selectSupplier.get()返回的就绪的IO事件个数，这个数最小是0，不会为负数
                    //所以就不会走下面的三个分支了，而返回的假如是SelectStrategy.SELECT，就说明现在既没有任务可执行，也没有IO事件
                    //所以要在select上阻塞等待IO事件到来

                    // return hasTasks ? selectSupplier.get() : SelectStrategy.SELECT
                    switch (selectStrategy.calculateStrategy(selectNowSupplier, hasTasks())) {
                        case SelectStrategy.CONTINUE:
                            continue;
                        case SelectStrategy.BUSY_WAIT:
                        case SelectStrategy.SELECT:
                            //走到这里说明没有用户提交的任务，所以等待IO事件到来即可
                            //调用select查询任务
                            //这里把false传进去，其实就是在select方法内部阻塞的意思
                            select(wakenUp.getAndSet(false));
                            //走到这里其实就意味着阻塞结束了，否则单线程执行器不可能向下执行
                            //然后得到是否阻塞的标志，唤醒一下selector，以便去执行其他任务
                            //其实我认为下面这个判断有点多余，因为如果wakeup为false，就不可能去唤醒selector
                            //而实际上wakeup会在上面的select中被设置成true，并且selector也会结束阻塞
                            //所以，下面这个判断有些多余
                            if (wakenUp.get()) {
                                selector.wakeup();
                            }
                        default:
                    }
                } catch (IOException e) {
                    //出现异常了在这里重建一下selector，然后处理异常
                    rebuildSelector0();
                    handleLoopException(e);
                    continue;
                }
                //被取消的key的个数
                cancelledKeys = 0;
                //是否需要再次select一下
                needsToSelectAgain = false;
                //下面就是根据具体的IO比来处理IO事件和用户提交的任务了
                //默认的IO占比为百分之50，也就是处理IO事件和用户的任务的事件各位一半，是相等的
                final int ioRatio = this.ioRatio;
                //如果IO占比为百分之百，就先执行IO事件，处理完所有IO事件了，再去执行用户的任务
                //这时候执行用户的任务就没有时间限制了，也是处理完就行
                if (ioRatio == 100) {
                    try {
                        //先处理IO事件
                        processSelectedKeys();
                    } finally {
                        //最后处理所有用户提交的任务
                        runAllTasks();
                    }
                } else {
                    //走到这里说明IO占比不是百分之百
                    //就要具体考虑了
                    final long ioStartTime = System.nanoTime();
                    try {
                        //这里仍然是先执行IO事件
                        processSelectedKeys();
                    } finally {
                        //当前得到的纳秒值，减去执行IO事件的纳秒值，这就得到了处理IO事件花费的时间
                        final long ioTime = System.nanoTime() - ioStartTime;
                        //然后就根据占比，得到执行任务的时间
                        //假如执行IO时间用了10秒，IO占比为百分之80
                        //那就意味着处理任务用百分之20
                        //100-80，再用20处以80，得到1/4，处理IO事件的时间乘以这个比例即可
                        // 10 * (100 - 80) / 80  = 200 / 80 =  2.5
                        runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
            try {
                //这里暂时不讲，最后一节课再讲
                //这两个方法都在父类SingleThreadEventExecutor中
                //还未在父类中引入，最后一节课才引入，所以暂时注释掉吧
                //作用就是判断单线程执行器，其实就是这个单线程线程池是否关闭了
//                if (isShuttingDown()) {
//                    closeAll();
//                    if (confirmShutdown()) {
//                        return;
//                    }
//                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
        }
    }

    //处理异常的方法
    private static void handleLoopException(Throwable t) {
        logger.warn("Unexpected exception in the selector loop.", t);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore.
        }
    }


    private void processSelectedKeys() throws Exception {
        if (selectedKeys != null) {
            //使用优化过后的key来处理IO时间，默认使用这个
            processSelectedKeysOptimized();
        } else {
            //这个则是没有优化的方法，之前我们一直使用的是这个方法
            processSelectedKeysPlain(selector.selectedKeys());
        }
    }

    protected void cleanup() {
        try {
            selector.close();
        } catch (IOException e) {
            logger.warn("Failed to close a selector.", e);
        }
    }

    void cancel(SelectionKey key) {
        key.cancel();
        cancelledKeys ++;
        if (cancelledKeys >= CLEANUP_INTERVAL) {
            cancelledKeys = 0;
            needsToSelectAgain = true;
        }
    }

    @Override
    protected Runnable pollTask() {
        Runnable task = super.pollTask();
        if (needsToSelectAgain) {
            selectAgain();
        }
        return task;
    }

    /**
     * @Author: ytrue
     * @Description:使用没有优化过的key处理IO事件，也就是我们之前一直使用的方法
     * 但是请大家注意，在一开始，我们就用反射替换了原生selector中的SelectionKey
     * 所以这里使用的其实也是这个key，但是是通过迭代器使用的，因为SelectedSelectionKeySet实现了迭代器接口
     * 但是用下面那个优化过key的方法，就会直接操纵SelectedSelectionKeySet对象中的数组了，而不使用迭代器
     * 这里注释就不添加了，都是很熟悉的操作了
     */
    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) throws Exception {
        if (selectedKeys.isEmpty()) {
            return;
        }
        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (;;) {
            final SelectionKey k = i.next();
            final Object a = k.attachment();
            i.remove();
            if (a instanceof AbstractNioChannel) {
                //具体处理IO事件的方法
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }
            //没有IO事件了，就打破循环
            if (!i.hasNext()) {
                break;
            }
            //是否需要再次select
            if (needsToSelectAgain) {
                selectAgain();
                //得到key集合
                selectedKeys = selector.selectedKeys();
                if (selectedKeys.isEmpty()) {
                    break;
                } else {
                    i = selectedKeys.iterator();
                }
            }
        }
    }

    /**
     * @Author: ytrue
     * @Description:使用优化过的key集合处理IO事件
     */
    private void processSelectedKeysOptimized() throws Exception {
        //这里直接操纵数组了
        for (int i = 0; i < selectedKeys.size; ++i) {
            final SelectionKey k = selectedKeys.keys[i];
            //相当于remove
            selectedKeys.keys[i] = null;
            //得到channel
            Object a = k.attachment();
            if (a instanceof AbstractNioChannel) {
                //具体处理IO事件的方法
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }
            if (needsToSelectAgain) {
                //清除数组中的key
                selectedKeys.reset(i + 1);
                selectAgain();
                i = -1;
            }
        }
    }

    /**
     * @Author: ytrue
     * @Description:重构之后的processSelectedKey方法
     * 这里就不添加注释了，都是老操作了
     */
    private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
        final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
        //这个if块中的逻辑后面再讲，和关闭channel有关
        if (!k.isValid()) {
            final EventLoop eventLoop;
            try {
                eventLoop = ch.eventLoop();
            } catch (Throwable ignored) {
                return;
            }
            if (eventLoop == this) {
                unsafe.close(unsafe.voidPromise());
            }
            if (eventLoop != this || eventLoop == null) {
                return;
            }
            unsafe.close(unsafe.voidPromise());
            return;
        }
        //下面就是处理对应的事件的操作了
        try {
            int readyOps = k.readyOps();
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps(ops);
                unsafe.finishConnect();
            }
            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                ch.unsafe().forceFlush();
            }
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }

    private static void processSelectedKey(SelectionKey k, NioTask<SelectableChannel> task) {
        int state = 0;
        try {
            task.channelReady(k.channel(), k);
            state = 1;
        } catch (Exception e) {
            k.cancel();
            invokeChannelUnregistered(task, k, e);
            state = 2;
        } finally {
            switch (state) {
                case 0:
                    k.cancel();
                    invokeChannelUnregistered(task, k, null);
                    break;
                case 1:
                    if (!k.isValid()) {
                        invokeChannelUnregistered(task, k, null);
                    }
                    break;
            }
        }
    }

    private void closeAll() {
        selectAgain();
        Set<SelectionKey> keys = selector.keys();
        Collection<AbstractNioChannel> channels = new ArrayList<AbstractNioChannel>(keys.size());
        for (SelectionKey k: keys) {
            Object a = k.attachment();
            if (a instanceof AbstractNioChannel) {
                channels.add((AbstractNioChannel) a);
            } else {
                k.cancel();
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                invokeChannelUnregistered(task, k, null);
            }
        }

        for (AbstractNioChannel ch: channels) {
            ch.unsafe().close(ch.unsafe().voidPromise());
        }
    }

    private static void invokeChannelUnregistered(NioTask<SelectableChannel> task, SelectionKey k, Throwable cause) {
        try {
            task.channelUnregistered(k.channel(), cause);
        } catch (Exception e) {
            logger.warn("Unexpected exception while running NioTask.channelUnregistered()", e);
        }
    }

    /**
     * @Author: ytrue
     * @Description:唤醒selector的方法
     */
    @Override
    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop && wakenUp.compareAndSet(false, true)) {
            selector.wakeup();
        }
    }

    /**
     * @Author: ytrue
     * @Description:立刻无阻塞select一下，看是否有IO事件
     */
    int selectNow() throws IOException {
        try {
            // selector.selectNow()  方法是 Java NIO 中的一个方法，用于选择已经准备好进行 I/O 操作的通道。它会立即返回一个数字，表示已经准备就绪的通道数量。
            // 返回的数字代表了已经就绪的通道的数量，可以是 0 或者一个正整数。
            // 如果返回的是 0，表示当前没有任何通道准备好进行 I/O 操作。如果返回的是一个正整数，表示有相应数量的通道已经准备好进行 I/O 操作。
            //需要注意的是， selectNow()  方法是非阻塞的，即使没有通道准备好，它也会立即返回。如果您希望在有通道准备好时进行阻塞等待，可以使用  selector.select()  方法。
            return selector.selectNow();
        } finally {
            if (wakenUp.get()) {
                //唤醒selector
                selector.wakeup();
            }
        }
    }
    /**
     * @Author: ytrue
     * @Description:真正select的方法，经过重构了，并且在该方法内解决了nio的空轮训bug
     * 这里传进来的参数为false
     */
    private void select(boolean oldWakenUp) throws IOException {
        //得到selector
        Selector selector = this.selector;
        try {
            //记录selector轮训过的次数，这个就是用来判断是否触发空轮训了
            int selectCnt = 0;
            //这里会得到当前时间
            long currentTimeNanos = System.nanoTime();
            //然后得到下一个定时任务要执行的时间
            //大家可以想一下，用户如果提交了定时任务，怎么可能到了执行时间还不执行呢？如果到了定时任务的执行时间
            //肯定就不能让单线程执行器在你selector上继续阻塞，应该去执行定时任务了
            long selectDeadLineNanos = currentTimeNanos + delayNanos(currentTimeNanos);

            for (;;) {
                //这里得到的就是selector要在select方法上阻塞的时间，然后把时间换算成毫秒
                long timeoutMillis = (selectDeadLineNanos - currentTimeNanos + 500000L) / 1000000L;

                if (timeoutMillis <= 0) {
                    //小于0说明已经超过定时任务要执行的事件了，要立刻去执行定时任务
                    if (selectCnt == 0) {
                        //这里会判断一下，看有没有轮训过IO事件
                        //如果一次都没有，就无阻塞的select一下，然后打破循环
                        //为什么要select一下呢？大家可以想一想，单线程处理器分配工作其实是很均衡的，同时也能体现出netty框架本身的职责
                        //说白了，它的主要职责是网络通信，所以，就算要去执行定时任务，也要先判断一下有没有IO事件
                        //执行了下面的方法，如果有IO事件，key其实就可以使用了，就可以去处理IO事件了
                        //所以这次打破循环后，单线程执行器就会去处理定时任务和IO事件
                        selector.selectNow();
                        selectCnt = 1;
                    }
                    //在这里打破循环，就意味着任务队列中有定时任务要被执行了
                    break;
                }
                //如果走到这里，说明还没有到达定时任务的执行时间，或者根本就没有定时任务，但这里会判断一下任务队列中是否有普通任务要去执行
                //所以这里又判断了一下，然后将是否阻塞的标志设置为不再阻塞
                //但是这里我还要再多说一句，wakenUp.compareAndSet(false, true)其实在SingleThreadEventExecutor的execute方法中
                //也会被调用到，就是向任务队列添加了任务之后，就会调用wakeup方法，而在wakeup方法中，就会执行wakenUp.compareAndSet(false, true)
                //这行代码，并且唤醒selector，这里就要考虑是哪个先执行成功了，如果单线程执行器中的wakeup方法先执行成功
                //这里就不会执行成功了，但一般来说，应该还是这里的先执行成功，
                if (hasTasks() && wakenUp.compareAndSet(false, true)) {
                    //selector.selectNow()和select方法类似，只是不会阻塞，并且返回有事件的个数
                    //和上面讲过的一样，这个方法执行后，如果有事件，key上其实就有事件可以得到了，所以这个方法break循环了之后
                    //在run方法内就可以执行IO事件了
                    //由此可以再次看到，这个netty的用户任务和IO事件的执行是很讲究的。会随时随地判断是否有IO事件
                    //毕竟是个网络通信框架，如果有IO事件，就会执行IO事件
                    //但是大家同时也可以认识到，在这个大的select方法中，已经有两个机会可以直接打破循环了
                    //这么做其实也很容易理解，就是能不让单线程执行器阻塞，就尽量不阻塞，有IO事件或者任务的时候，
                    //直接去执行就行
                    selector.selectNow();
                    selectCnt = 1;
                    break;
                }
                //走到这里就说明没有IO事件和用户提交的任务，selector就可以在select方法阻塞了
                //并且会阻塞到下一个定时任务开始执行之前
                int selectedKeys = selector.select(timeoutMillis);

                //阻塞完了之后，轮训的次数加1
                selectCnt ++;

                //走到这里说明短暂的阻塞结束了，又要开始新一轮的判断
                //判断有没有就绪的IO事件，判断有没有用户提交的任务和定时任务，只要有就可以打破循环了
                //如果什么都没有就继续循环，和我们重构前的select方法一样
                if (selectedKeys != 0 || oldWakenUp || wakenUp.get() || hasTasks() || hasScheduledTasks()) {
                    break;
                }
                if (Thread.interrupted()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Selector.select() returned prematurely because " +
                                     "Thread.currentThread().interrupt() was called. Use " +
                                     "NioEventLoop.shutdownGracefully() to shutdown the NioEventLoop.");
                    }
                    selectCnt = 1;
                    break;
                }

                //得到当前时间
                long time = System.nanoTime();
                //这里的意思是，既然上面的selector已经阻塞结束了，那就看看当前时间减去阻塞结束的时间是不是大于之前得到当前时间
                //如果大于就意味着真的阻塞了那么久，才开始继续向下执行的，如果时间差小于阻塞的时间，就意味着没有阻塞，直接就返回了
                //这就是触发了空轮训bug
                if (time - TimeUnit.MILLISECONDS.toNanos(timeoutMillis) >= currentTimeNanos) {
                    selectCnt = 1;
                } else if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
                           selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
                    //轮训次数大于阈值了，触发空轮训bug了，所以可以重建selector了
                    selector = selectRebuildSelector(selectCnt);
                    //重建之后，把轮训次数置为1
                    selectCnt = 1;
                    break;
                }
                currentTimeNanos = time;
            }
            if (selectCnt > MIN_PREMATURE_SELECTOR_RETURNS) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Selector.select() returned prematurely {} times in a row for Selector {}.",
                            selectCnt - 1, selector);
                }
            }
        } catch (CancelledKeyException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector {} - JDK bug?",
                        selector, e);
            }
        }
    }

    /**
     * @Author: ytrue
     * @Description:重建selector的方法
     */
    private Selector selectRebuildSelector(int selectCnt) throws IOException {
        logger.warn(
                "Selector.select() returned prematurely {} times in a row; rebuilding Selector {}.",
                selectCnt, selector);
        //在这里重建selector
        rebuildSelector();
        Selector selector = this.selector;
        //重建之后立刻select，看看是否有IO事件就绪
        selector.selectNow();
        return selector;
    }

    /**
     * @Author: ytrue
     * @Description:再次select一下的方法
     */
    private void selectAgain() {
        needsToSelectAgain = false;
        try {
            selector.selectNow();
        } catch (Throwable t) {
            logger.warn("Failed to update SelectionKeys.", t);
        }
    }
}

