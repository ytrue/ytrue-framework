package com.ytrue.job.admin.core.thread;

import com.ytrue.job.admin.core.conf.XxlJobAdminConfig;
import com.ytrue.job.admin.core.trigger.TriggerTypeEnum;
import com.ytrue.job.admin.core.trigger.XxlJobTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ytrue
 * @date 2023-08-29 9:48
 * @description xxl-job服务器的重点类，在这个类中初始化了两个线程池。一个快，一个慢，要被执行的任务会被包装成触发器
 * 任务，提交给这两个线程池中的一个，然后由线程池去执行者触发器任务，在任务中会进行远程调用。
 */
public class JobTriggerPoolHelper {

    private static final Logger logger = LoggerFactory.getLogger(JobTriggerPoolHelper.class);


    /**
     * 下面这两个快慢线程池没有什么本质的不同，都是线程池而已，只不过快线程池的最大线程数为200
     * 慢线程池的最大线程数为100，任务队列也是如此
     * 并且会根据任务执行的耗时，来决定下次任务执行的时候是要让快线程池来执行还是让慢线程池来执行
     * 默认是选择使用快线程池来执行
     * 到这里大家应该明白了，所谓的快慢线程池并不是说线程执行任务的快慢，而是任务的快慢决定了线程的快慢
     * 直接来说，执行耗时较短的任务，我们可以称它为快速任务，而执行这些任务的线程池，就被成为了快线程池
     * 如果任务耗时较长，就给慢线程池来执行
     */
    //快线程池
    private ThreadPoolExecutor fastTriggerPool = null;
    //慢线程池
    private ThreadPoolExecutor slowTriggerPool = null;


    /**
     * 在这里创建了两个快慢线程池
     */
    public void start() {
        //快线程池，最大线程数为200
        fastTriggerPool = new ThreadPoolExecutor(
                10,
                XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> new Thread(r, "xxl-job, admin JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode()));

        //慢线程池，最大线程数为100
        slowTriggerPool = new ThreadPoolExecutor(
                10,
                XxlJobAdminConfig.getAdminConfig().getTriggerPoolSlowMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> new Thread(r, "xxl-job, admin JobTriggerPoolHelper-slowTriggerPool-" + r.hashCode()));
    }


    /**
     * 关闭线程池
     */
    public void stop() {
        fastTriggerPool.shutdownNow();
        slowTriggerPool.shutdownNow();
        logger.info(">>>>>>>>> xxl-job trigger thread pool shutdown success.");
    }


    /**
     * 这个方法就是远程调用的起点，很重要的入口方法。JobInfoController类中的triggerJob方法会调用到
     * 这里，还有JobScheduleHelper类中也会调用到该方法。当然，在该方法外面还有一层trigger方法，这个方法就在本类中
     * 属于是该方法的外层方法
     * 该方法的各个参数分别输任务Id，触发的枚举类型(其实就是手动触发的意思，手动调用该任务，执行一次)，
     * 失败重试次数，分片参数，执行器方法参数，执行器的地址列表
     * 最后，还是想再解释一下，因为是第一个手写版本，所以很有些参数我们都用不到，但是后面会陆续重构完整
     *
     * @param jobId
     * @param triggerType
     * @param failRetryCount
     * @param executorShardingParam
     * @param executorParam
     * @param addressList
     */
    public void addTrigger(final int jobId,
                           final TriggerTypeEnum triggerType,
                           final int failRetryCount,
                           final String executorShardingParam,
                           final String executorParam,
                           final String addressList) {


        //默认先选用快线程池
        ThreadPoolExecutor triggerPool_ = fastTriggerPool;
        //用任务Id从，慢执行的Map中得到该job对应的慢执行次数
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        //这里就是具体判断了，如果慢执行次数不为null，并且一分钟超过10了，就选用慢线程池来执行该任务
        if (jobTimeoutCount != null && jobTimeoutCount.get() > 10) {
            //选用慢线程池了
            triggerPool_ = slowTriggerPool;
        }
        //在这里就把任务提交给线程池了，在这个任务执行一个触发器任务，把刚才传进来的job的各种信息整合到一起
        //在触发器任务中，会进行job的远程调用，这个调用链还是比较短的，执行流程也很清晰
        triggerPool_.execute(() -> {
            //再次获取当前时间，这个时间后面会用到
            long start = System.currentTimeMillis();
            try {
                //触发器任务开始执行了，在该方法内部，会进行远程调用
                XxlJobTrigger.trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                //这里再次获得当前的分钟数，这个分钟数会和刚才上面得到的那个分钟数做对比
                long minTim_now = System.currentTimeMillis() / 60000;
                //这里就用到了两个分钟数做对比，如果两个分钟数不等，说明过去了一分钟
                //而慢执行Map中的数据是一分钟清理一次，所以这里就把慢执行Map清空
                //注意，这个清空的动作是线程池中的线程来执行的，并且这个动作是在finally代码块中执行的
                //也就意味着是在上面的触发器任务执行完毕后才进行清空操作
                if (minTim != minTim_now) {
                    minTim = minTim_now;
                    jobTimeoutCountMap.clear();
                }
                //在这里用当前毫秒值减去之前得到的毫秒值
                long cost = System.currentTimeMillis() - start;
                //判断任务的执行时间是否超过500毫秒了
                //这里仍然要结合上面的finally代码块来理解，因为触发器任务执行完了才会执行finally代码块中的
                //代码，所以这时候也就能得到job的执行时间了
                if (cost > 500) {
                    //超过500毫秒了，就判断当前执行的任务为慢执行任务，所以将它在慢执行Map中记录一次
                    //Map的key为jobid，value为慢执行的次数
                    AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                    if (timeoutCount != null) {
                        //慢执行的次数加一
                        timeoutCount.incrementAndGet();
                    }
                }
            }
        });
    }


    /**
     * 获取当前的系统时间，这里计算出来的其实是系统当前的分钟数，下面马上就会用到
     */
    private volatile long minTim = System.currentTimeMillis() / 60000;


    /**
     * 如果有任务出现慢执行情况了，就会被记录在该Map中
     * 所谓慢执行，就是执行的时间超过了500毫秒，该Map的key为job的id，value为慢执行的次数
     * 如果一分钟慢执行的次数超过了10次，该任务就会被交给慢线程池的来执行
     * 而该Map也会一分钟清空一次，来循环记录慢执行的情况
     */
    private volatile ConcurrentMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();


    /**
     * 静态成员变量，说明该变量也只会初始化一次，并且根据修饰符来看，该成员变量也不会直接对外暴露
     * 而是通过下面的两个方法间接在外部调用
     */
    private static final JobTriggerPoolHelper helper = new JobTriggerPoolHelper();

    //启动方法
    public static void toStart() {
        helper.start();
    }

    //停止方法
    public static void toStop() {
        helper.stop();
    }


    /**
     * 该方法会对外暴露，然后调用到该类内部的addTrigger方法，该方法的作用就是把要执行的job，包装成
     * 一个触发器任务，在触发器任务中进行远程调用，然后在执行器那一端执行该job
     * 方法的参数在该类的addTrigger方法处已经详细解释过了，这里就不再添加注释了
     *
     * @param jobId
     * @param triggerType
     * @param failRetryCount
     * @param executorShardingParam
     * @param executorParam
     * @param addressList
     */
    public static void trigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam, String executorParam, String addressList) {

        helper.addTrigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
    }

}
