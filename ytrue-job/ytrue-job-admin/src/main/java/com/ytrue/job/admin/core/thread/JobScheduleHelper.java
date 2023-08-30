package com.ytrue.job.admin.core.thread;

import com.ytrue.job.admin.core.conf.XxlJobAdminConfig;
import com.ytrue.job.admin.core.cron.CronExpression;
import com.ytrue.job.admin.core.model.XxlJobInfo;
import com.ytrue.job.admin.core.scheduler.MisfireStrategyEnum;
import com.ytrue.job.admin.core.scheduler.ScheduleTypeEnum;
import com.ytrue.job.admin.core.trigger.TriggerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-08-29 9:48
 * @description 该类就完全可以说是xxl-job服务端的最核心类了
 * 任务的调度就是在这个类中执行的，线程会不停地扫描数据库，看哪些任务该执行了。执行完毕后，还要计算下次执行的时间
 */
public class JobScheduleHelper {
    private static final Logger logger = LoggerFactory.getLogger(JobScheduleHelper.class);

    /**
     * 创建当前类的对象
     */
    private static JobScheduleHelper instance = new JobScheduleHelper();

    /**
     * 把当前类的对象暴露出去
     *
     * @return
     */
    public static JobScheduleHelper getInstance() {
        return instance;
    }

    /**
     * 5s的意思，这个成员变量下面就会用到
     */
    public static final long PRE_READ_MS = 5000;

    /**
     * 下面这个成员变量就是哦用来调度的线程，其实在该类中工作的都是线程，并没有创建线程池
     * 这个线程会在一个循环中不停地查询数据库，看哪些任务该执行了，哪些任务已经过了执行时间，然后进行相应的处理
     * 这个线程也会提交给触发器线程池执行触发器任务，但是这个并不是该线程的主要工作。该线程的主要工作是扫描数据库，查询到期的执行任务
     * 并且维护任务的下一次执行时间
     */
    private Thread scheduleThread;


    /**
     * 这个就是时间轮线程，还记得我之前在外面添加的注解吗？时间轮并不只是线程，也并不只是容器
     * 容器和线程结合在一起，构成了可以运行的时间轮
     * 这个时间轮线程就是用来主要向触发器线程池提交触发任务的
     * 它提交的任务是从Map中获得的，而Map中的任务是由上面的调度线程添加的，具体逻辑会在下面的代码中讲解
     */
    private Thread ringThread;

    /**
     * 下面这两个是纯粹的标记，就是用来判断线程是否停止的
     */
    private volatile boolean scheduleThreadToStop = false;
    private volatile boolean ringThreadToStop = false;

    /**
     * 这个就是时间轮的容器，该容器中的数据是由scheduleThread线程添加的
     * 但是移除是由ringThread线程移除的
     * Map的key为时间轮中任务的执行时间，value是需要执行的定时任务的集合，这个集合中的数据就是需要执行的定时任务的id
     */
    private volatile static Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();


    /**
     * 启动scheduleThread线程
     */
    public void start() {
        scheduleThread = new Thread(() -> {
            try {
                //这里的逻辑非常简单，就是起到了一个对其时间的效果，因为这个调度线程的调度周期是5秒
                //所以如果现在的时间不是整数秒的话，就让调度线程睡到整数秒再启动
                //比如System.currentTimeMillis()%1000计算得到的时间是得到了一个300ms的值
                //那么5000-300，就是让线程睡4700ms，就会到达一个整数秒了，然后直接启动就行。
                TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000);
            } catch (InterruptedException e) {
                if (!scheduleThreadToStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            logger.info(">>>>>>>>> init xxl-job admin scheduler success.");

            //这里就是xxl大神写死的一个每秒可以调度的任务量。这里默认每个定时任务执行大概耗时50毫秒，1秒为1000毫秒，所以，1秒中可以执行20个定时任务
            //但是执行定时任务，在调度中心实际上就是用快慢线程池执行触发器任务，因为定时任务真正执行还是在执行器那一端，
            //所以，在加上快慢线程池拥有的线程总数，假如快慢线程池都拉满了，都到达了最大线程数，那就是200加上100，总的线程数为300
            //每秒每个线程可以执行20个定时任务，现在有300个线程，所以，最后可以得到，每秒最多可以调度6000个定时任务
            //6000就是一个限制的最大值，如果调度线程要扫描数据库，从数据库取出要执行的任务，每次最多可以取6000个
            //注意啊，我这里并没有使用每秒可以取6000个，大家不要搞混了，取出来的任务要交给线程池执行的时候，每秒最多可以执行6000个
            //数据库取出任务限制6000，也只是为了配合这个限制的数量
            // 100 + 200 * 20
            int preReadCount = (XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax() + XxlJobAdminConfig.getAdminConfig().getTriggerPoolSlowMax()) * 20;
            //开始进入循环了
            while (!scheduleThreadToStop) {
                //得到调度任务的开始时间
                long start = System.currentTimeMillis();
                //下面这几个步骤都和数据库有关，因为xxl-job是使用数据库来实现分布式锁的
                //既然是数据库锁，就不能自动提交事物，所以，这里要手动设置一下
                Connection conn = null;
                //这里就是要设置不自动提交
                Boolean connAutoCommit = null;
                //执行sql语句的变量
                PreparedStatement preparedStatement = null;
                //这个变量下面要用到，就是用来判断是否从数据库中读取到了数据，读取到了就意味着有任务要执行
                //这里默认为true
                boolean preReadSuc = true;

                try {
                    //获得连接
                    conn = XxlJobAdminConfig.getAdminConfig().getDataSource().getConnection();
                    //是否自动提交
                    connAutoCommit = conn.getAutoCommit();
                    //这里就设置为不自动提交了
                    conn.setAutoCommit(false);
                    //设置sql语句，获得数据库锁，这知识就不讲了，数据库的基础知识
                    preparedStatement = conn.prepareStatement("select * from xxl_job_lock where lock_name = 'schedule_lock' for update");
                    //开始执行sql语句，得到数据库锁
                    preparedStatement.execute();
                    //获得当前时间，这里要把这个时间和上面那个start做一下区分
                    //这两个时间变量的作用不同
                    //现在这个时间变量是用来得到要调度的任务的
                    //上面那个是最后用来做判断，看看扫描数据库耗费了多少时间
                    long nowTime = System.currentTimeMillis();

                    //这里就可以很明显的看出来，去数据库查询要执行的任务是，是用当前时间加上5秒，把这个时间段的数据全部都取出来，并不是只取当前时间的
                    //这里的preReadCount为6000，之前计算的这个限制在这里用上了
                    //但是，为什么一下子把5秒内未执行的任务都取出来呢？可以继续向下看，同时带着这个思考，也许下面的代码逻辑可以为你解答这个问题
                    //这里记得去看一下xml中对应的sql语句，其实还是判断任务的下一次执行时间小于传进去的这个时间
                    // SELECT <include refid="Base_Column_List" />
                    //		FROM xxl_job_info AS t
                    //		WHERE t.trigger_status = 1
                    //			and t.trigger_next_time <![CDATA[ <= ]]> #{maxNextTime}
                    //		ORDER BY id ASC
                    //		LIMIT #{pagesize}
                    // 10   + 5 = 15 小于 15 秒的任务全部取出来
                    List<XxlJobInfo> scheduleList = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().scheduleJobQuery(nowTime + PRE_READ_MS, preReadCount);
                    //判空操作
                    if (scheduleList != null && scheduleList.size() > 0) {
                        //循环处理每一个任务
                        for (XxlJobInfo jobInfo : scheduleList) {
                            //这里做了一个判断，刚才得到的当前时间，是不是大于任务的下一次执行时间加上5秒，为什么会出现这种情况呢？
                            //让我们仔细想一想，本来，一个任务被调度执行了，就会计算出它下一次的执行时机，然后更新数据库中的任务的下一次执行时间
                            //但请大家思考另外一种情况，如果服务器宕机了呢？本来上一次要执行的任务，却没有执行，比如这个任务要在第5秒执行，但是服务器在第4秒宕机了
                            //重新恢复运行后，已经是第12秒了，现在去数据库中查询任务，12 > 5 + 5，就是if括号中的不等式，这样一来，是不是就查到了执行时间比当前时间还小的任务
                            //所以，情况要考虑全面
                            // 超过5秒的任务，这里对过期任务的处理方式！！！！
                            if (nowTime > jobInfo.getTriggerNextTime() + PRE_READ_MS) {
                                logger.warn(">>>>>>>>>>> xxl-job, schedule misfire, jobId = " + jobInfo.getId());
                                //既然有过期的任务，就要看看怎么处理，是直接不处理，还是其他的处理方式。这里程序默认的是什么也不做，既然过期了，就过期吧
                                MisfireStrategyEnum misfireStrategyEnum = MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), MisfireStrategyEnum.DO_NOTHING);

                                //当然，这里也是再判断了一次，万一失败策略是立刻重试一次，那就立刻执行一次任务
                                if (MisfireStrategyEnum.FIRE_ONCE_NOW == misfireStrategyEnum) {
                                    //在这里立刻执行一次任务,调度过期补偿
                                    JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.MISFIRE, -1, null, null, null);
                                    logger.debug(">>>>>>>>>>> xxl-job, schedule push trigger : jobId = " + jobInfo.getId());
                                }
                                //在这里把过期任务的下次执行时间刷新一下，放到下一次来执行
                                refreshNextValidTime(jobInfo, new Date());
                            }
                            //这里得到的就是要执行的任务的下一次执行时间同样也小于了当前时间，但是这里和上面的不同是，没有超过当前时间加5秒的那个时间
                            //现在大家应该都清楚了，上面加的那个5秒实际上就是调度周期，每一次处理的任务都是当前任务加5秒这个时间段内的
                            //这一次得到的任务仅仅是小于当前时间，但是并没有加上5秒，说明这个任务虽然过期了但仍然是在当前的调度周期中
                            //比如说这个任务要在第2秒执行，但是服务器在第1秒宕机了，恢复之后已经是第4秒了，现在任务的执行时间小于了当前时间，但是仍然在5秒的调度器内
                            //所以直接执行即可
                            else if (nowTime > jobInfo.getTriggerNextTime()) {
                                //把任务交给触发器去远程调用,Cron触发
                                JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.CRON, -1, null, null, null);
                                logger.debug(">>>>>>>>>>> xxl-job, schedule push trigger : jobId = " + jobInfo.getId());
                                //刷新该任务下一次的执行时间
                                refreshNextValidTime(jobInfo, new Date());
                                //下面这个分之中的任务就是比较正常的，但是又有些特殊的，
                                //首先判断它是不是在启动的状态，然后判断这个任务的下一次执行时间是否小于这个执行周期，注意，上面的refreshNextValidTime方法已经把该任务的
                                //下一次执行时间更新了。如果更新后的时间仍然小于执行周期，说明这个任务会在执行周期中再执行一次，当然，也可能会执行多次，
                                //这时候，就不让调度线程来处理这个任务了，而是把它提交给时间轮，让时间轮去执行
                                //不知道看到这里，大家有没有一个疑问，为什么需要时间轮去执行呢？调度线程自己去把任务给触发器线程池执行不行吗？还有，为什么要设计一个5秒
                                //的调度周期呢？xxl-job定时任务的调度精度究竟准确吗？大家可以先自己想想，有一个很明确的方向，就是有的任务可能会很耗时，或者某个地方查询数据库阻塞太久了
                                //耽误了后续任务的执行，大家可以先想想，到最后我会为大家做一个总结。
                                if (jobInfo.getTriggerStatus() == 1 && nowTime + PRE_READ_MS > jobInfo.getTriggerNextTime()) {
                                    //计算该任务要放在时间轮的刻度，也就是在时间轮中的执行时间，注意哦，千万不要被这里的取余给搞迷惑了
                                    //这里的余数计算结果为0-59，单位是秒，意味着时间轮有60个刻度，一个代表一秒。
                                    //调度线程是按调度周期来处理任务的，举个例子，调度线程从0秒开始启动，第5秒为一个周期，把这5秒要执行的任务交给时间轮了
                                    //就去处理下一个调度周期，千万不要把调度线程处理调度任务时不断增加的调度周期就是增长的时间，调度线程每次扫描数据库不会耗费那么多时间
                                    //这个时间是作者自己设定的，并且调度线程也不是真的只按整数5秒去调度任务
                                    //实际上，调度线程从0秒开始工作，扫描0-5秒的任务，调度这些任务耗费了1秒，再次循环时，调度线程就会1秒开始，处理1-6秒的任务
                                    //虽说是1-6秒，但是1-5秒的任务都被处理过了，但是请大家想一想，有些任务也仅仅只是被执行了一次，如果有一个任务在0-5秒调度器内被执行了
                                    //但是该任务每1秒执行一次，从第1秒开始m，那它是不是会在调度期内执行多次？可是上一次循环它可能最多只被执行了两次，一次在调度线程内，一次在时间轮内
                                    //还有几次并未执行呢，所以要交给下一个周期去执行，但是这时候它的下次执行时间还在当前时间的5秒内，如果下个周期直接从6秒开始
                                    //这个任务就无法执行了，大家可以仔细想想这个过程
                                    //时间轮才是真正按照时间增长的速度去处理定时任务的
                                    int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);
                                    //把定时任务的信息，就是它的id放进时间轮
                                    pushTimeRing(ringSecond, jobInfo.getId());
                                    //刷新定时任务的下一次的执行时间，注意，这里传进去的就不再是当前时间了，而是定时任务现在的下一次执行时间
                                    //因为放到时间轮中就意味着它要执行了，所以计算新的执行时间就行了
                                    refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));
                                }
                            }
                            //最后，这里得到的就是最正常的任务，也就是执行时间在当前时间之后，但是又小于执行周期的任务
                            //上面的几个判断，都是当前时间大于任务的下次执行时间，实际上都是在过期的任务中做判断
                            else {
                                //这样的任务就很好处理了，反正都是调度周期，也就是当前时间5秒内要执行的任务，所以直接放到时间轮中就行
                                //计算出定时任务在时间轮中的刻度，其实就是定时任务执行的时间对应的秒数
                                //随着时间流逝，时间轮也是根据当前时间秒数来获取要执行的任务的，所以这样就可以对应上了
                                int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);
                                //放进时间轮中
                                pushTimeRing(ringSecond, jobInfo.getId());
                                //刷新定时任务下一次的执行时间
                                refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));

                            }
                        }
                        //最后再更新一下所有的任务
                        for (XxlJobInfo jobInfo : scheduleList) {
                            // UPDATE xxl_job_info
                            //		SET
                            //			trigger_last_time = #{triggerLastTime},
                            //			trigger_next_time = #{triggerNextTime},
                            //			trigger_status = #{triggerStatus}
                            //		WHERE id = #{id}
                            XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().scheduleUpdate(jobInfo);
                        }
                    } else {
                        //走到这里，说明根本就没有从数据库中扫描到任何任务，把preReadSuc设置为false
                        preReadSuc = false;
                    }

                } catch (Exception e) {
                    if (!scheduleThreadToStop) {
                        logger.error(">>>>>>>>>>> xxl-job, JobScheduleHelper#scheduleThread error:{}", e);
                    }
                } finally {
                    //下面就是再次和数据库有关的操作了，提交事物，释放锁，再次设置非手动提交，释放资源等等
                    //这里就自己看看吧
                    if (conn != null) {
                        try {
                            conn.commit();
                        } catch (SQLException e) {
                            if (!scheduleThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                        try {
                            conn.setAutoCommit(connAutoCommit);
                        } catch (SQLException e) {
                            if (!scheduleThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            if (!scheduleThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                    if (null != preparedStatement) {
                        try {
                            preparedStatement.close();
                        } catch (SQLException e) {
                            if (!scheduleThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }
                }

                //再次得到当然时间，然后减去开始执行扫面数据库任务的开始时间
                //就得到了执行扫面数据库，并且调度任务的总耗时
                long cost = System.currentTimeMillis() - start;
                //这里有一个判断，1000毫秒就是1秒，如果总耗时小于1秒，就默认数据库中可能没多少数据
                //线程就不必工作得那么繁忙，所以下面要让线程休息一会，然后再继续工作
                if (cost < 1000) {
                    try {
                        //下面有一个三元运算，判断preReadSuc是否为true，如果扫描到数据了，就让该线程小睡一会儿，最多睡1秒
                        //如果根本就没有数据，就说明5秒的调度器内没有任何任务可以执行，那就让线程最多睡5秒，把时间睡过去，过5秒再开始工作
                        TimeUnit.MILLISECONDS.sleep((preReadSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis() % 1000);
                    } catch (InterruptedException e) {
                        if (!scheduleThreadToStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
            }
            logger.info(">>>>>>>>>>> xxl-job, JobScheduleHelper#scheduleThread stop");
        });
        //设置守护线程，启动线程
        scheduleThread.setDaemon(true);
        scheduleThread.setName("xxl-job, admin JobScheduleHelper#scheduleThread");
        scheduleThread.start();


        //下面这个就是时间轮的工作线程
        ringThread = new Thread(() -> {
            while (!ringThreadToStop) {
                try {
                    //这里让线程睡一会，作用还是比较明确的，因为该线程是时间轮线程，时间轮执行任务是按照时间刻度来执行的
                    //如果这一秒内的所有任务都调度完了，但是耗时只用了500毫秒，剩下的500毫秒就只好睡过去，等待下一个整秒到来
                    //再继续开始工作。System.currentTimeMillis() % 1000计算出来的结果如果是500毫秒，1000-500=500
                    //线程就继续睡500毫秒，如果System.currentTimeMillis() % 1000计算出来的是0，说明现在是整秒，那就睡1秒，等到下个
                    //工作时间再开始工作
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                } catch (InterruptedException e) {
                    if (!ringThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                try {
                    //先定义一个集合变量，刚才已经强调过了，时间轮是一个Map容器，Map的key是定时任务要执行的时间，value是定时任务的JobID的集合
                    //到了固定的时间，要把对应时刻的定时任务从集合中取出来，所以自然也要用集合来存放这些定时任务的ID
                    List<Integer> ringItemData = new ArrayList<>();
                    //获取当前时间的秒数
                    int nowSecond = Calendar.getInstance().get(Calendar.SECOND);
                    //下面这里很有意思，如果我们计算出来的是第3秒，时间轮线程会把第2秒，和第3秒的定时任务都取出来，一起执行
                    //这里肯定会让大家感到困惑，时间轮不是按照刻度走的吗？如果走到3秒的刻度，说明2秒的任务已经执行完了，为什么还要再拿出来？
                    //这是因为考虑到定时任务的调度情况了，如果时间轮某个刻度对应的定时任务太多，本来该最多1秒就调度完的，结果调度了2秒，直接把下一个刻度跳过了
                    //这样不就出错了？所以，每次执行的时候要把前一秒的也取出来，检查一下看是否有任务，这也算是一个兜底的方法
                    for (int i = 0; i < 2; i++) {
                        //循环了两次，第一次取出当前刻度的任务，第二次取出前一刻度的任务
                        //注意，这里取出的时候，定时任务就从时间轮中被删除了
                        List<Integer> tmpData = ringData.remove((nowSecond + 60 - i) % 60);
                        if (tmpData != null) {
                            //把定时任务的ID数据添加到上面定义的集合中
                            ringItemData.addAll(tmpData);
                        }
                    }
                    logger.debug(">>>>>>>>>>> xxl-job, time-ring beat : " + nowSecond + " = " + Arrays.asList(ringItemData));
                    //判空操作
                    if (ringItemData.size() > 0) {
                        for (int jobId : ringItemData) {

                            //在for循环中处理定时任务，让触发器线程池开始远程调用这些任务
                            JobTriggerPoolHelper.trigger(jobId, TriggerTypeEnum.CRON, -1, null, null, null);
                        }
                        //最后清空集合
                        ringItemData.clear();
                    }
                } catch (Exception e) {
                    if (!ringThreadToStop) {
                        logger.error(">>>>>>>>>>> xxl-job, JobScheduleHelper#ringThread error:{}", e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> xxl-job, JobScheduleHelper#ringThread stop");
        });
        //到这里可以总结一下了，总的来说，xxljob之所以把任务调度搞得这么复杂，判断了多种情况，引入时间轮
        //就是考虑到某些任务耗时比较严重，结束时间超过了后续任务的执行时间，所以要经常判断前面有没有未执行的任务
        ringThread.setDaemon(true);
        ringThread.setName("xxl-job, admin JobScheduleHelper#ringThread");
        ringThread.start();
    }


    /**
     * 把定时任务放到时间轮中
     *
     * @param ringSecond
     * @param jobId
     */
    private void pushTimeRing(int ringSecond, int jobId) {
        List<Integer> ringItemData = ringData.get(ringSecond);
        if (ringItemData == null) {
            ringItemData = new ArrayList<>();
            ringData.put(ringSecond, ringItemData);
        }
        ringItemData.add(jobId);
        logger.debug(">>>>>>>>>>> xxl-job, schedule push time-ring : " + ringSecond + " = " + Arrays.asList(ringItemData));
    }

    /**
     * 刷新定时任务下一次的执行时间
     *
     * @param jobInfo
     * @param fromTime
     * @throws Exception
     */
    private void refreshNextValidTime(XxlJobInfo jobInfo, Date fromTime) throws Exception {
        Date nextValidTime = generateNextValidTime(jobInfo, fromTime);
        if (nextValidTime != null) {
            // 上次调度时间
            jobInfo.setTriggerLastTime(jobInfo.getTriggerNextTime());
            // 下次调度时间
            jobInfo.setTriggerNextTime(nextValidTime.getTime());
        } else {
            jobInfo.setTriggerStatus(0);
            jobInfo.setTriggerLastTime(0);
            jobInfo.setTriggerNextTime(0);
            logger.warn(">>>>>>>>>>> xxl-job, refreshNextValidTime fail for job: jobId={}, scheduleType={}, scheduleConf={}",
                    jobInfo.getId(), jobInfo.getScheduleType(), jobInfo.getScheduleConf());
        }
    }


    /**
     * 集合cron表达式计算定时任务下一次的执行时间
     *
     * @param jobInfo
     * @param fromTime
     * @return
     * @throws Exception
     */
    public static Date generateNextValidTime(XxlJobInfo jobInfo, Date fromTime) throws Exception {
        // 获取调度类型
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);

        // 如果是用cron表达式
        if (ScheduleTypeEnum.CRON == scheduleTypeEnum) {
            return new CronExpression(jobInfo.getScheduleConf()).getNextValidTimeAfter(fromTime);
        }
        // 如果是按照固定频率
        else if (ScheduleTypeEnum.FIX_RATE == scheduleTypeEnum) {
            return new Date(fromTime.getTime() + Integer.valueOf(jobInfo.getScheduleConf()) * 1000);
        }
        return null;
    }


    /**
     * 停止任务调度器的方法，其实就是终止本类的两个线程
     */
    public void toStop() {
        scheduleThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (scheduleThread.getState() != Thread.State.TERMINATED) {
            scheduleThread.interrupt();
            try {
                scheduleThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        boolean hasRingData = false;
        if (!ringData.isEmpty()) {
            for (int second : ringData.keySet()) {
                List<Integer> tmpData = ringData.get(second);
                if (tmpData != null && tmpData.size() > 0) {
                    hasRingData = true;
                    break;
                }
            }
        }
        if (hasRingData) {
            try {
                TimeUnit.SECONDS.sleep(8);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        ringThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (ringThread.getState() != Thread.State.TERMINATED) {
            // interrupt and wait
            ringThread.interrupt();
            try {
                ringThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        logger.info(">>>>>>>>>>> xxl-job, JobScheduleHelper stop");
    }

}
