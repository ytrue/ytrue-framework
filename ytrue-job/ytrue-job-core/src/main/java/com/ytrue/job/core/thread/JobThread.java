package com.ytrue.job.core.thread;

import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.biz.model.TriggerParam;
import com.ytrue.job.core.executor.XxlJobExecutor;
import com.ytrue.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-08-28 14:05
 * @description 该类就是用来真正执行定时任务的，并且是一个定时任务对应着一个JobThread对象
 * 其实说一个并不太准确，比如，有一个定时任务每2s执行一次，那么在执行器这一端，定时任务对应的JobThread对象一但创建了
 * 就会只执行这个定时任务，但是有可能这个任务比较耗时，3秒还没执行完，那么之后每2秒要执行的这个定时任务可能就会放在JobThread对象中的
 * 队列中等待执行，由此也就引申出了阻塞策略，是选择覆盖还是直接丢弃等等
 * 该类继承了thread，本身就是一个线程
 */
public class JobThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(JobThread.class);

    /**
     * 定时任务的ID
     */
    private int jobId;

    /**
     * 封装了定时任务方法的对象，别忘了，bean对象的初始化方法和销毁方法也在该类封装着
     */
    private IJobHandler handler;

    /**
     * 存放触发器参数的一个队列，也就是我们上面刚刚解释过的，有可能一个任务比较耗时，3秒还没执行完，但调度周期是2秒，
     * 那么之后每2秒要执行的这个定时任务可能就会放在JobThread对象中的队列中等待执行，其实存放的就是触发器参数，触发器参数中有待执行的定时任务的名称
     */
    private LinkedBlockingQueue<TriggerParam> triggerQueue;

    /**
     * 线程终止的标记
     */
    private volatile boolean toStop = false;
    /**
     * 线程停止的原因
     */
    private String stopReason;
    /**
     * 线程是否正在工作的标记
     */
    private boolean running = false;
    /**
     * 该线程的空闲时间，默认为0
     */
    private int idleTimes = 0;

    public JobThread(int jobId, IJobHandler handler) {
        this.jobId = jobId;
        this.handler = handler;
        //初始化队列
        this.triggerQueue = new LinkedBlockingQueue<>();
        //设置工作线程名称
        this.setName("xxl-job, JobThread-" + jobId + "-" + System.currentTimeMillis());
    }

    public IJobHandler getHandler() {
        return handler;
    }


    /**
     * 把触发器参数放进队列中的方法
     *
     * @param triggerParam
     * @return
     */
    public ReturnT<String> pushTriggerQueue(TriggerParam triggerParam) {
        //在这里放进队列中
        triggerQueue.add(triggerParam);
        //返回成功结果
        return ReturnT.SUCCESS;
    }


    /**
     * 终止该线程的方法
     *
     * @param stopReason
     */
    public void toStop(String stopReason) {
        //把线程终止标记设为true
        this.toStop = true;
        this.stopReason = stopReason;
    }

    /**
     * 断线程是否有任务，并且是否正在运行，这个方法会和阻塞策略一起使用，但现在并没有引入阻塞策略，所以该方法暂时用不上
     *
     * @return
     */
    public boolean isRunningOrHasQueue() {
        return running || triggerQueue.size() > 0;
    }

    /**
     * 当前线程启动之后会执行的run方法
     */
    @Override
    public void run() {
        try {
            //下面就是常规逻辑了，终于要开始执行定时任务的方法了
            //但如果IJobHandler对象中封装了bean对象的初始化方法，并且该定时任务注解中也声明了初始化方法要执行
            //就在这里反射调用bean对象的初始化方法
            handler.init();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        while(!toStop){
            //现在就要在一个循环中不断的从触发器队列中取出待执行的定时任务，开始执行
            //线程是否工作的标记，默认为false
            running = false;
            //这个是线程的空闲时间
            idleTimes++;
            //先声明一个触发器参数变量
            TriggerParam triggerParam;
            try {
                //从触发器参数队列中取出一个触发器参数对象
                //这里是限时的阻塞获取，如果超过3秒没获取到，就不阻塞了
                // 将首个元素从队列中弹出，如果队列是空的，就返回null
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
                if (triggerParam!=null) {
                    //走到这里，说明获得了触发器参数，这时候就把线程正在执行的标记设置为true
                    running = true;
                    //空闲时间也可以置为0了
                    idleTimes = 0;
                    //通过反射执行了定时任务，终于在这里执行了
                    handler.execute();
                } else {
                    //走到这里说明触发器队列中没有数据，也就意味着没有要执行的定时任务
                    //如果线程的空闲时间大于30次，这里指的是循环的次数，每循环一次空闲时间就自增1，
                    //有定时任务被执行空闲时间就清零，不可能没任务线程空转，太浪费资源了
                    if (idleTimes > 30) {
                        //而且触发器队列也没有数据
                        if(triggerQueue.size() == 0) {
                            //就从缓存JobThread线程的jobThreadRepository这个Map中移除缓存的JobThread线程
                            //在移除的时候，会调用该线程的toStop方法和interrupt方法，让线程真正停下来
                            XxlJobExecutor.removeJobThread(jobId, "excutor idel times over limit.");
                        }
                    }
                }
            } catch (Throwable e) {
                if (toStop) {
                    //这里就打印一下即可，源码还会记录日志内容，这里就不记录了，xxljob要记录日志的地方太多了
                    //我就先不记那么多了
                    logger.info("<br>----------- JobThread toStop, stopReason:" + stopReason);
                }
            }
        }
        //代码走到这里就意味着退出了线程工作的while循环，虽然线程还未完全执行完run方法，但是已经意味着线程要停止了
        //执行一下销毁方法即可，其实本类的过程很复杂，还涉及到了执行器端的回调线程，把执行的详细结果回调给调度中心
        //我都省略了，后续会迭代完整
        try {
            //执行bean对象的销毁方法
            handler.destroy();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        logger.info(">>>>>>>>>>> xxl-job JobThread stoped, hashCode:{}", Thread.currentThread());
    }
}
