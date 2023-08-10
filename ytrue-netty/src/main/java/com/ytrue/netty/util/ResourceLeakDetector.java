package com.ytrue.netty.util;

import com.ytrue.netty.util.internal.EmptyArrays;
import com.ytrue.netty.util.internal.PlatformDependent;
import com.ytrue.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static com.ytrue.netty.util.internal.StringUtil.*;

/**
 * @author ytrue
 * @date 2023-08-10 9:09
 * @description 内存泄漏检测的核心类，这个类翻译过来叫内存泄漏探测器，该类定义了一些检测的属性，真正具备检测功能的类是该类的内部类
 */
public class ResourceLeakDetector<T> {
    //用于配置老版本的内存泄漏检测级别的参数
    private static final String PROP_LEVEL_OLD = "io.netty.leakDetectionLevel";

    //用于配置新版本的内存泄漏检测级别的参数
    private static final String PROP_LEVEL = "io.netty.leakDetection.level";

    //默认的内存泄漏检测级别
    private static final Level DEFAULT_LEVEL = Level.SIMPLE;

    //这个属性就是用于配置Record数量的
    private static final String PROP_TARGET_RECORDS = "io.netty.leakDetection.targetRecords";

    //Record链表的值为4，默认值就是4，这个值会赋值给上面的那个属性代表的参数
    private static final int DEFAULT_TARGET_RECORDS = 4;

    //内存泄漏检测的另一个级别
    private static final String PROP_SAMPLING_INTERVAL = "io.netty.leakDetection.samplingInterval";

    //以上面级别执行内存泄漏检测时，会有一个128的频率来执行检测任务，换句话说，就是并不是每次都执行内存泄露检测
    //每隔128次才检测一次
    private static final int DEFAULT_SAMPLING_INTERVAL = 128;

    //默认值4会被赋值给该属性
    private static final int TARGET_RECORDS;

    //这个属性会被128赋值，代表Level.ADVANCED或者Level.SIMPLE级别的内存泄漏检测将会以128的频率来报告内存是否泄露
    static final int SAMPLING_INTERVAL;

    /**
     * @Author: ytrue
     * @Description:内存泄露的检测级别
     */
    public enum Level {

        //关闭内存泄露检测
        DISABLED,

        //最简单的级别，如果发生了内存泄露，并没有详细的信息报告，只是告诉用户是否发生了内存泄露
        //调用轨迹等等全都没有
        SIMPLE,

        //稍微高级一点的内存泄露检测级别，一般都会使用这个级别来进行内存泄露检测
        ADVANCED,

        //最高的内存泄露检测级别
        //通常情况下不会应用的
        PARANOID;


        static Level parseLevel(String levelStr) {
            String trimmedLevelStr = levelStr.trim();
            for (Level l : values()) {
                if (trimmedLevelStr.equalsIgnoreCase(l.name()) || trimmedLevelStr.equals(String.valueOf(l.ordinal()))) {
                    return l;
                }
            }
            return DEFAULT_LEVEL;
        }
    }

    private static Level level;

    private static final Logger logger = LoggerFactory.getLogger(ResourceLeakDetector.class);

    //下面仍然是静态代码块给一些属性赋值，这些属性和要赋的值在上面的成员变量中都有。就不再重复了
    //大家简单看看就行
    static {
        final boolean disabled;
        //是否关闭内存泄露检测，默认为fasle，不关闭检测
        if (SystemPropertyUtil.get("io.netty.noResourceLeakDetection") != null) {
            disabled = SystemPropertyUtil.getBoolean("io.netty.noResourceLeakDetection", false);
            logger.debug("-Dio.netty.noResourceLeakDetection: {}", disabled);
            logger.warn(
                    "-Dio.netty.noResourceLeakDetection is deprecated. Use '-D{}={}' instead.",
                    PROP_LEVEL, DEFAULT_LEVEL.name().toLowerCase());
        } else {
            disabled = false;
        }
        Level defaultLevel = disabled? Level.DISABLED : DEFAULT_LEVEL;
        String levelStr = SystemPropertyUtil.get(PROP_LEVEL_OLD, defaultLevel.name());
        levelStr = SystemPropertyUtil.get(PROP_LEVEL, levelStr);
        Level level = Level.parseLevel(levelStr);
        TARGET_RECORDS = SystemPropertyUtil.getInt(PROP_TARGET_RECORDS, DEFAULT_TARGET_RECORDS);
        SAMPLING_INTERVAL = SystemPropertyUtil.getInt(PROP_SAMPLING_INTERVAL, DEFAULT_SAMPLING_INTERVAL);
        ResourceLeakDetector.level = level;
        if (logger.isDebugEnabled()) {
            logger.debug("-D{}: {}", PROP_LEVEL, level.name().toLowerCase());
            logger.debug("-D{}: {}", PROP_TARGET_RECORDS, TARGET_RECORDS);
        }
    }


    @Deprecated
    public static void setEnabled(boolean enabled) {
        setLevel(enabled? Level.SIMPLE : Level.DISABLED);
    }


    public static boolean isEnabled() {
        return getLevel().ordinal() > Level.DISABLED.ordinal();
    }


    /**
     * @Author: ytrue
     * @Description:设置级别的方法
     */
    public static void setLevel(Level level) {
        if (level == null) {
            throw new NullPointerException("level");
        }
        ResourceLeakDetector.level = level;
    }

    /**
     * @Author: ytrue
     * @Description:返回级别的方法
     */
    public static Level getLevel() {
        return level;
    }

    //这个set集合很重要，这个集合会被每一个弱引用对象使用，把弱引用对象添加到该集合中，当我们判断内存是被释放了时，就会依据这个集合中的
    //内容进行判断
    private final Set<DefaultResourceLeak<?>> allLeaks =
            Collections.newSetFromMap(new ConcurrentHashMap<DefaultResourceLeak<?>, Boolean>());
    //弱引用队列，注意，该队列同样也会被每一个弱引用对象使用
    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>();
    //报告调用轨迹的map
    private final ConcurrentMap<String, Boolean> reportedLeaks = PlatformDependent.newConcurrentHashMap();
    //要检测的资源的类型，其实就是ByteBuf，这个类型在最开始就会被传进来，就是在AbstractByteBuf类中的静态成员变量中执行的
    private final String resourceType;
    //这个属性就是在执行内存泄露检测时，执行检测的频率，会被128赋值，就是每隔128次才检测一次
    private final int samplingInterval;

    /**
     * @Author: ytrue
     * @Description:下面都是一些构造函数，就不再详细注释了
     */
    @Deprecated
    public ResourceLeakDetector(Class<?> resourceType) {
        this(simpleClassName(resourceType));
    }


    @Deprecated
    public ResourceLeakDetector(String resourceType) {
        this(resourceType, DEFAULT_SAMPLING_INTERVAL, Long.MAX_VALUE);
    }


    @Deprecated
    public ResourceLeakDetector(Class<?> resourceType, int samplingInterval, long maxActive) {
        this(resourceType, samplingInterval);
    }


    @SuppressWarnings("deprecation")
    public ResourceLeakDetector(Class<?> resourceType, int samplingInterval) {
        this(simpleClassName(resourceType), samplingInterval, Long.MAX_VALUE);
    }


    @Deprecated
    public ResourceLeakDetector(String resourceType, int samplingInterval, long maxActive) {
        if (resourceType == null) {
            throw new NullPointerException("resourceType");
        }

        this.resourceType = resourceType;
        this.samplingInterval = samplingInterval;
    }


    @Deprecated
    public final ResourceLeak open(T obj) {
        return track0(obj);
    }


    //该方法可以返回一个ResourceLeakTracker弱引用对象，该弱引用对象记录了ByteBuf的每一次调用轨迹
    @SuppressWarnings("unchecked")
    public final ResourceLeakTracker<T> track(T obj) {
        return track0(obj);
    }

    /**
     * @Author: ytrue
     * @Description:该方法用来创建真正的ResourceLeakTracker弱引用对象
     * 其中参数obj实际上就是要被包装的ByteBuf，在这里就是PooledDirectByteBuf。
     */
    @SuppressWarnings("unchecked")
    private DefaultResourceLeak track0(T obj) {
        Level level = ResourceLeakDetector.level;
        //先判断内存泄露的检测级别，如果级别为DISABLED，意味着关闭内存泄露检测，直接返回null即可
        if (level == Level.DISABLED) {
            return null;
        }
        //这里判断一下内存泄露的检测级别是否小于PARANOID，因为PARANOID是最高的检测级别，一般不会用到，而走到这里说明内存泄露检测肯定没有
        //关闭，所以肯定就是SIMPLE或者ADVANCED级别。至于枚举类的大小判断，则是根据顺序进行的，在上面的枚举对象小于在下面的枚举对象
        if (level.ordinal() < Level.PARANOID.ordinal()) {
            //这里有一个随机数的判断，还记得我们讲解成员变量时的那个128吗？这里就是在0到128之间取随机数，取到0的概率就是128分之1
            //这就模拟出了一个内存泄露的检测频率，就是在这两个等级下，每隔128次才检测一次
            if ((PlatformDependent.threadLocalRandom().nextInt(samplingInterval)) == 0) {
                //报告内存是否泄露，其实就是打印一下弱引用队列中是否有弱引用对象
                //如果有，就打印一下这个弱引用对象记录的调用轨迹
                reportLeak();
                //返回一个弱引用对象，该弱引用对象持有了ByteBuf，弱引用队列，还有set集合
                return new DefaultResourceLeak(obj, refQueue, allLeaks);
            }
            //走到这里说明频率还未达到，也就可能没有满足128次，所以直接返回null，并不检测内存是否泄露
            //注意，这里有一个地方要想清楚，就是用户每创建一个ByteBuf，实际上就是申请了一块内存，这块内存最终还是要释放的。所以ByteBuf也要包装一下
            //但是，在不同的内存泄露检测级别下，并不是每次创建的ByteBuf都会被包装成可以检测内存泄露的ByteBuf对象
            //而是按照相应的规则来进行包装，可能每次都包装，并且创建对应的弱引用对象，也可能创建128次ByteBuf，才会忽然包装一次，检测一次内存泄露
            //这一点一定要想明白
            return null;
        }
        //走到这里说明对应的内存泄露检测级别为PARANOID，每次都要进行检测，并且包装ByteBUf
        //打印内存泄露检测报告
        reportLeak();
        //返回弱引用对象
        return new DefaultResourceLeak(obj, refQueue, allLeaks);
    }

    /**
     * @Author: ytrue
     * @Description:循环清除弱引用队列中的对象，该方法会在reportLeak方法内调用
     */
    private void clearRefQueue() {
        for (;;) {
            //从队列中取出弱引用对象
            @SuppressWarnings("unchecked")
            DefaultResourceLeak ref = (DefaultResourceLeak) refQueue.poll();
            if (ref == null) {
                break;
            }
            //清除弱引用对象中的弱引用
            ref.dispose();
        }
    }

    /**
     * @Author: ytrue
     * @Description:报告内存泄露检测信息
     */
    private void reportLeak() {
        //这里做了一个日志级别的判断，如果不是error级别的，就清空弱引用队列，然后直接退出该方法
        //由此可以看出，报告内存泄露，必须要求日记级别为error。否则就不报告
        if (!logger.isErrorEnabled()) {
            clearRefQueue();
            return;
        }
        //循环取出弱引用队列中的弱引用对象，然后打印弱引用对象的内存泄露信息
        for (;;) {
            //从弱引用队列中取出弱引用对象
            @SuppressWarnings("unchecked")
            DefaultResourceLeak ref = (DefaultResourceLeak) refQueue.poll();
            //如果弱引用对象为null，说明队列中没有内容，没有发生内存泄露
            if (ref == null) {
                //退出该循环
                break;
            }
            //这里如果返回false，说明弱引用对象已经从集合中删除了，这意味着这个弱引用持有的ByteBuf已经顺利被回收了
            //没有发生内存泄露，所以跳过这次循环即可
            if (!ref.dispose()) {
                continue;
            }
            //走到这里说明确实发生了内存泄露，要把弱引用对象的信息打印一下，打印之前，要获得被打印的信息
            //这里的toString方法，实际上就是把弱引用对象内的Record链表拼成字符串信息
            //然后就得到了ByteBuf的调用轨迹
            String records = ref.toString();
            //这里把得到的轨迹放入一个map中，如果返回null，说明这个泄露的问题还没有放入过该map中
            if (reportedLeaks.putIfAbsent(records, Boolean.TRUE) == null) {
                //判断该字符串是否为空
                if (records.isEmpty()) {
                    //如果为空说明没有记录调用轨迹，那该内存泄露对应的级别可能为SIMPLE级别的，这时候，就打印一下日志，输出一下内存泄露了即可
                    reportUntracedLeak(resourceType);
                } else {
                    //这里就是有调用轨迹的内存泄露报告，要打印一下Record链表的信息
                    reportTracedLeak(resourceType, records);
                }
            }
        }
    }
    //下面这两个方法就是对应了是否有轨迹信息
    //该方法会打印调用轨迹
    protected void reportTracedLeak(String resourceType, String records) {
        logger.error(
                "LEAK: {}.release() was not called before it's garbage-collected. " +
                "See https://netty.io/wiki/reference-counted-objects.html for more information.{}",
                resourceType, records);
    }
    //该方法不会打印轨迹信息
    protected void reportUntracedLeak(String resourceType) {
        logger.error("LEAK: {}.release() was not called before it's garbage-collected. " +
                     "Enable advanced leak reporting to find out where the leak occurred. " +
                     "To enable advanced leak reporting, " +
                     "specify the JVM option '-D{}={}' or call {}.setLevel() " +
                     "See https://netty.io/wiki/reference-counted-objects.html for more information.",
                resourceType, PROP_LEVEL, Level.ADVANCED.name().toLowerCase(), simpleClassName(this));
    }


    @Deprecated
    protected void reportInstancesLeak(String resourceType) {
    }

    //这个内部类就是要创建的弱引用的类型
    //这个弱引用类型对应着Bytebuf，当ByteBuf没有调用它的release方法就被垃圾回收了时，就会发生内存泄露了，创建的这个弱引用对象持有了
    //ByteBuf的弱引用，就会被放到弱引用队列中。注意，是这个弱引用对象被放到弱引用队列中，并不是ByteBuf被放到弱引用队列中
    //要搞清楚这一点。
    @SuppressWarnings("deprecation")
    private static final class DefaultResourceLeak<T>
            extends WeakReference<Object> implements ResourceLeakTracker<T>, ResourceLeak {

        //Record链表头节点的原子更新器
        //Record链表就是记录调用轨迹的链表，每一个调用轨迹都会被封装到Record对象中，组成一个链表
        @SuppressWarnings("unchecked")
        private static final AtomicReferenceFieldUpdater<DefaultResourceLeak<?>, Record> headUpdater =
                (AtomicReferenceFieldUpdater)
                        AtomicReferenceFieldUpdater.newUpdater(DefaultResourceLeak.class, Record.class, "head");

        //记录被丢弃的Record的数量
        @SuppressWarnings("unchecked")
        private static final AtomicIntegerFieldUpdater<DefaultResourceLeak<?>> droppedRecordsUpdater =
                (AtomicIntegerFieldUpdater)
                        AtomicIntegerFieldUpdater.newUpdater(DefaultResourceLeak.class, "droppedRecords");
        //Record链表的头节点
        @SuppressWarnings("unused")
        private volatile Record head;
        //被删掉的Record的数量
        @SuppressWarnings("unused")
        private volatile int droppedRecords;
        //这个set集合会暂时存储所有的弱引用对象，当弱引用对象对应的ByteBuf被正确释放了，该弱引用对象就会从这个集合中清除
        private final Set<DefaultResourceLeak<?>> allLeaks;
        //该属性用来记录弱引用对象持有的弱引用对象的hash值，也就是PooledDirectByteBuf的hash值
        //之所以不直接把持有的对象赋值给这个属性，是为了防止强引用
        private final int trackedHash;


        //构造函数
        DefaultResourceLeak(
                Object referent,
                ReferenceQueue<Object> refQueue,
                Set<DefaultResourceLeak<?>> allLeaks) {
            //这里会调用WeakReference的构造方法
            //该方法就可让ByteBuf在被垃圾回收时，把该弱引用对象添加到弱引用队列中了
            //除非ByteBuf在被垃圾回收前已经把持有的弱引用从父类清除了
            //也就是把referent从父类清除
            super(referent, refQueue);
            assert referent != null;
            //该弱引用对象持有的是哪个对象，在这里就会被确定，但确定的方式并不是直接把对象引用过来，而是得到那个对象的hash值
            trackedHash = System.identityHashCode(referent);
            //把创建的弱引用对象加入到set集合中。当该弱引用对象监控的ByteBuf执行了release方法后，也就是SimpleLeakAwareByteBuf中的release
            //方法，会一路调用到本类中的close方法，close方法会关闭对相应的ByteBuf的内存泄露检测，就是通过hash值判断是否是同一个ByteBuf
            //然后会将弱引用对象从set集合中清除，并且清除弱引用对象中对Bytebuf的引用。这就表明监控的ByteBuf被正确释放了
            allLeaks.add(this);
            //初始化该弱引用对象中的record链表的头节点
            headUpdater.set(this, new Record(Record.BOTTOM));
            this.allLeaks = allLeaks;
        }

        /**
         * @Author: ytrue
         * @Description:记录ByteBuf的调用轨迹，该方法会在AdvancedLeakAwareByteBuf类中通过静态方法recordLeakNonRefCountingOperation
         * 调用，具体逻辑可以去AdvancedLeakAwareByteBuf中查看
         */
        @Override
        public void record() {
            record0(null);
        }

        /**
         * @Author: ytrue
         * @Description:作用同上
         */
        @Override
        public void record(Object hint) {
            record0(hint);
        }

        /**
         * @Author: ytrue
         * @Description:该方法就是用来真正记录ByteBuf的调用轨迹的
         */
        private void record0(Object hint) {
            //TARGET_RECORDS的值为4，这个值其实就是record单向链表的长度，这就意味着这个链表最好最多有4个节点
            //当链表的长度不足4的时候，新的record节点会被添加到链表中，但是如果链表的长度已经等于4了，这时候又有新的调用轨迹被记录了
            //这时候就会根据一个概率，用最新的record节点替换掉链表的头节点。这个概率在该方法内就会看到了。
            //注意，这里是替换头节点的概率，这也仅仅是个概率，所以，如果没能替换头节点，那么最新的调用轨迹record节点就会直接添加到头节点之前
            //但是这样一来，单向链表的长度就会越来越长了，占用的内存也会越来越多
            //如果每一个ByteBuf的record的链表都很长，占用的内存还是相当恐怖的
            if (TARGET_RECORDS > 0) {
                Record oldHead;
                Record prevHead;
                Record newHead;
                boolean dropped;
                do {
                    //headUpdater是record链表的头节点更新器，通过这个成员变量可以得到头节点
                    //如果头节点为null，就说明这个弱引用对象内部没有什么调用轨迹，已经被正确的close了。因为record链表的头节点
                    //在构造器中就会被初始化，而在close方法内会被置为null，所以为null就没必要继续向下执行了
                    //直接退出即可
                    if ((prevHead = oldHead = headUpdater.get(this)) == null) {
                        return;
                    }
                    //得到当前record链表的长度
                    final int numElements = oldHead.pos + 1;
                    //判断链表长度是否大于等于4
                    if (numElements >= TARGET_RECORDS) {
                        //走到这里意味着链表长度大于4
                        final int backOffFactor = Math.min(numElements - TARGET_RECORDS, 30);
                        //这个就是我们上面说的所谓的替换头节点的概率，大家可以带入具体的数值计算一下
                        //总之，就是下面的这个随机数如果不等于0了，就返回true，就意味着要替换链表的头节点
                        //只有当这个随机数等于0的时候，才不会替换头节点，而是把新的轨迹节点record添加到链表中
                        if (dropped = PlatformDependent.threadLocalRandom().nextInt(1 << backOffFactor) != 0) {
                            //在这里得到旧的头节点的下一个节点
                            prevHead = oldHead.next;
                        }
                    } else {
                        //走到这里说明不会替换链表头节点
                        dropped = false;
                    }
                    //创建一个新的record对象，当作新的头节点，并且把下一个节点传进去，将链表连接起来
                    //这里我要再啰嗦一下，如果没有经过随机数的那个分支，就意味着没执行prevHead = oldHead.next这行代码
                    //那么旧的头节点还保持不变，这样的话再执行下面这行代码，就会直接创建一个新的头节点，然后把它后旧的头节点连接起来即可
                    //这就是直接添加新的头节点，而没有替换旧的头节点
                    newHead = hint != null ? new Record(prevHead, hint) : new Record(prevHead);
                    //在这里用原子更新器实现头节点的替换
                } while (!headUpdater.compareAndSet(this, oldHead, newHead));
                //如果替换了头节点，说明丢弃了一个record记录，所以要给计数器加1
                if (dropped) {
                    droppedRecordsUpdater.incrementAndGet(this);
                }
            }
        }

        /**
         * @Author: ytrue
         * @Description:清除弱引用对象中的引用，并且判断弱引用对象的状态
         */
        boolean dispose() {
            //将弱引用对象中的引用清除掉
            clear();
            //这里要从set集合中将弱引用对象移除，注意，这里有一个逻辑很重要。因为该方法通常是在ByteBuf被正确释放后调用的，
            //而ByteBuf在被释放时会从allLeaks集合中清除弱引用对象
            //所以这时候如果再次执行下面的方法，如果执行失败，则返回false，意味着之前释放ByteBuf的时候已经删除成功了，代表ByteBuf被正确释放了
            //如果下面的方法返回true，则意味着之前没有正确释放ByteBuf，这就出问题了
            //要理清楚这个逻辑
            //而且，通过前面的reportLeak方法，大家应该看得出来，实际上就是通过allLeaks集合中是否还存在弱引用对象
            //来判断ByteBuf是否被正确的释放和回收的，如果没有正确释放，当然就意味着发生了内存泄露
            //同时也应该意识到，在Netty中判断内存是否泄露，并不完全依据弱引用队列中是否有弱引用对象
            //而是该弱引用对象是否还在allLeaks集合中
            return allLeaks.remove(this);
        }

        /**
         * @Author: ytrue
         * @Description:关闭对ByteBuf对象的内存泄露的检测的方法
         */
        @Override
        public boolean close() {
            //首先就要从set集合中将弱引用对象清除，也就是清除自身
            if (allLeaks.remove(this)) {
                //成功清除了弱引用对象，就将弱引用对象中对ByteBuf引用也清除干净
                clear();
                //将record链表的头节点置为null
                headUpdater.set(this, null);
                //返回true即可
                return true;
            }
            //走到这里说明从集合中清除自身失败，返回false
            return false;
        }

        /**
         * @Author: ytrue
         * @Description:关闭对ByteBuf对象的内存泄露的检测的方法，当然，这个方法只是套了层壳，内部会执行真正的close方法
         */
        @Override
        public boolean close(T trackedObject) {
            //通过hash值的判断，确定要处理的是同一个ByteBuf对象
            assert trackedHash == System.identityHashCode(trackedObject);
            try {
                //执行真正的close方法
                return close();
            } finally {
                //在Java中，有极小的概率会出现一种问题，就是当一个对象正执行它的方法时，该对象有可能会被判定为不可达从而被回收掉。
                //具体的解释就是，当这个对象拥有的属性不再被使用或者改变，这个对象就有可能在执行方法的时候被垃圾回收
                //就比如现在正在关闭某一个ByteBuf的内存泄露检测，这个时候ByteBuf实际上已经没什么用了，持有的内存也许已经被释放了
                //所以就有可能在close方法还未执行完的时候就被垃圾回收了。如果是这样，那这个ByteBuf对应的弱引用对象就会被放到
                //弱引用队列中，因为很有可能垃圾回收的时候clear方法也没执行呢。也没有从allLeaks结合中删除呢
                //当调用前面的reportLeak方法时，dispose仍然会返回true。
                //这样一来，就会出现内存泄露的误判，所以，在这里有了下面这个方法，用一个synchronized同步锁保证该ByteBuf对象不会被垃圾回收
                //并且在finally块中，这样就保证了该ByteBuf对象要被回收也一定是执行完了close方法再被回收
                reachabilityFence0(trackedObject);
            }
        }

        private static void reachabilityFence0(Object ref) {
            if (ref != null) {
                synchronized (ref) {
                    // Empty synchronized is ok: https://stackoverflow.com/a/31933260/1151521
                }
            }
        }

        /**
         * @Author: ytrue
         * @Description:这个方法就是把record链表转换成字符串信息的方法，都是一些拼接的操作，我就不注释了。大家可以自己看看
         */
        @Override
        public String toString() {
            Record oldHead = headUpdater.getAndSet(this, null);
            if (oldHead == null) {
                return EMPTY_STRING;
            }
            final int dropped = droppedRecordsUpdater.get(this);
            int duped = 0;
            int present = oldHead.pos + 1;
            StringBuilder buf = new StringBuilder(present * 2048).append(NEWLINE);
            buf.append("Recent access records: ").append(NEWLINE);
            int i = 1;
            Set<String> seen = new HashSet<String>(present);
            for (; oldHead != Record.BOTTOM; oldHead = oldHead.next) {
                String s = oldHead.toString();
                if (seen.add(s)) {
                    if (oldHead.next == Record.BOTTOM) {
                        buf.append("Created at:").append(NEWLINE).append(s);
                    } else {
                        buf.append('#').append(i++).append(':').append(NEWLINE).append(s);
                    }
                } else {
                    duped++;
                }
            }
            if (duped > 0) {
                buf.append(": ")
                        .append(duped)
                        .append(" leak records were discarded because they were duplicates")
                        .append(NEWLINE);
            }
            if (dropped > 0) {
                buf.append(": ")
                        .append(dropped)
                        .append(" leak records were discarded because the leak record count is targeted to ")
                        .append(TARGET_RECORDS)
                        .append(". Use system property ")
                        .append(PROP_TARGET_RECORDS)
                        .append(" to increase the limit.")
                        .append(NEWLINE);
            }
            buf.setLength(buf.length() - NEWLINE.length());
            return buf.toString();
        }
    }

    private static final AtomicReference<String[]> excludedMethods =
            new AtomicReference<String[]>(EmptyArrays.EMPTY_STRINGS);

    public static void addExclusions(Class clz, String ... methodNames) {
        Set<String> nameSet = new HashSet<String>(Arrays.asList(methodNames));
        for (Method method : clz.getDeclaredMethods()) {
            if (nameSet.remove(method.getName()) && nameSet.isEmpty()) {
                break;
            }
        }
        if (!nameSet.isEmpty()) {
            throw new IllegalArgumentException("Can't find '" + nameSet + "' in " + clz.getName());
        }
        String[] oldMethods;
        String[] newMethods;
        do {
            oldMethods = excludedMethods.get();
            newMethods = Arrays.copyOf(oldMethods, oldMethods.length + 2 * methodNames.length);
            for (int i = 0; i < methodNames.length; i++) {
                newMethods[oldMethods.length + i * 2] = clz.getName();
                newMethods[oldMethods.length + i * 2 + 1] = methodNames[i];
            }
        } while (!excludedMethods.compareAndSet(oldMethods, newMethods));
    }

    //Record内部类，这个类继承了Throwable，因此可以得到方法的调用轨迹，也就是调用栈
    //就通过Throwable.getStackTrace方法即可，最后打印出来的也就是这个信息
    private static final class Record extends Throwable {
        private static final long serialVersionUID = 6065153674892850720L;

        private static final Record BOTTOM = new Record();

        private final String hintString;
        private final Record next;
        private final int pos;

        Record(Record next, Object hint) {
            hintString = hint instanceof ResourceLeakHint ? ((ResourceLeakHint) hint).toHintString() : hint.toString();
            this.next = next;
            this.pos = next.pos + 1;
        }

        Record(Record next) {
            hintString = null;
            this.next = next;
            this.pos = next.pos + 1;
        }

        private Record() {
            hintString = null;
            next = null;
            pos = -1;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(2048);
            if (hintString != null) {
                buf.append("\tHint: ").append(hintString).append(NEWLINE);
            }
            //获取调用轨迹，也就是调用栈
            StackTraceElement[] array = getStackTrace();
            //Skip the first three elements.
            //跳过前面三个元素
            out: for (int i = 3; i < array.length; i++) {
                StackTraceElement element = array[i];
                //Strip the noisy stack trace elements.
                //略去一些不必要的信息
                String[] exclusions = excludedMethods.get();
                for (int k = 0; k < exclusions.length; k += 2) {
                    if (exclusions[k].equals(element.getClassName())
                        && exclusions[k + 1].equals(element.getMethodName())) {
                        continue out;
                    }
                }
                buf.append('\t');
                buf.append(element.toString());
                buf.append(NEWLINE);
            }
            return buf.toString();
        }
    }
}
