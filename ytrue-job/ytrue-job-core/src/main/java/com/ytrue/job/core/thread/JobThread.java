package com.ytrue.job.core.thread;

import com.ytrue.job.core.biz.model.HandleCallbackParam;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.biz.model.TriggerParam;
import com.ytrue.job.core.context.XxlJobContext;
import com.ytrue.job.core.context.XxlJobHelper;
import com.ytrue.job.core.executor.XxlJobExecutor;
import com.ytrue.job.core.handler.IJobHandler;
import com.ytrue.job.core.log.XxlJobFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
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
     * 线程是否正在工作的标记，注意，这个标记并不是只线程是否启动或销毁
     * 而是指线程是否正在执行定时任务
     */
    private boolean running = false;
    /**
     * 该线程的空闲时间，默认为0
     */
    private int idleTimes = 0;


    //定时任务的地址id集合
    private Set<Long> triggerLogIdSet;


    public JobThread(int jobId, IJobHandler handler) {
        this.jobId = jobId;
        this.handler = handler;
        //初始化队列
        this.triggerQueue = new LinkedBlockingQueue<>();
        //初始化集合
        this.triggerLogIdSet = Collections.synchronizedSet(new HashSet<>());
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

        //先判断set集合中包含定时任务的地址id吗，如果包含，就说明定时任务正在执行
        if (triggerLogIdSet.contains(triggerParam.getLogId())) {
            logger.info(">>>>>>>>>>> repeate trigger job, logId:{}", triggerParam.getLogId());
            //返回失败信息，定时任务重复了
            return new ReturnT<>(ReturnT.FAIL_CODE, "repeate trigger job, logId:" + triggerParam.getLogId());
        }
        //没包含则将定时任务的日志id放到集合中
        triggerLogIdSet.add(triggerParam.getLogId());

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
     * 断线程是否有任务，并且是否正在运行
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
        while (!toStop) {
            //现在就要在一个循环中不断的从触发器队列中取出待执行的定时任务，开始执行
            //线程是否工作的标记，默认为false
            running = false;
            //这个是线程的空闲时间
            idleTimes++;
            //先声明一个触发器参数变量
            TriggerParam triggerParam = null;
            try {
                //从触发器参数队列中取出一个触发器参数对象
                //这里是限时的阻塞获取，如果超过3秒没获取到，就不阻塞了
                // 将首个元素从队列中弹出，如果队列是空的，就返回null
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
                if (triggerParam != null) {
                    //走到这里，说明获得了触发器参数，这时候就把线程正在执行的标记设置为true
                    running = true;
                    //空闲时间也可以置为0了
                    idleTimes = 0;

                    // -----------------------------------------------------日志相关处理
                    //因为定时任务要执行了，所以要把它的日志ID先从set集合中删除
                    triggerLogIdSet.remove(triggerParam.getLogId());

                    //接下来就是一系列的处理执行器端定时任务执行的日志操作
                    //先根据定时任务的触发时间和定时任务的日志id，创建一个记录定时任务日的文件名
                    // D:\data\applogs\xxl-job\jobhandler\2023-11-11\1.log
                    String logFileName = XxlJobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTime()), triggerParam.getLogId());


                    //然后创建一个定时任务上下文对象
                    XxlJobContext xxlJobContext = new XxlJobContext(
                            // 任务id
                            triggerParam.getJobId(),
                            // 任务参数
                            triggerParam.getExecutorParams(),
                            // 日志文件名称
                            logFileName,
                            // 分片索引
                            triggerParam.getBroadcastIndex(),
                            // 分片总数
                            triggerParam.getBroadcastTotal());

                    //先把创建出来的定时任务上下文对象存储到执行定时任务线程的私有容器中
                    XxlJobContext.setXxlJobContext(xxlJobContext);
                    //这里会向logFileName文件中记录一下日志，记录的就是下面的这句话，定时任务开始执行了
                    XxlJobHelper.log("<br>----------- xxl-job job execute start -----------<br>----------- Param:" + xxlJobContext.getJobParam());

                    //通过反射执行了定时任务，终于在这里执行了
                    handler.execute();

                    //定时任务执行了，所以这里要判断一下执行结果是什么，注意，这里的XxlJobContext上下文对象
                    //从创建的时候就默认执行结果为成功。在源码中，在这行代码之前其实还有任务执行超时时间的判断，开启一个子线程去执行定时任务
                    //然后再判断任务执行成功了没，如果没成功XxlJobHelper类就会修改上下文对象的执行结果。等我们引入任务超时的功能后，这里的逻辑就会更丰富了
                    if (XxlJobContext.getXxlJobContext().getHandleCode() <= 0) {
                        // 作业句柄结果丢失。
                        XxlJobHelper.handleFail("job handle result lost.");
                    } else {
                        //走到这里意味着定时任务执行成功了，从定时任务上下文中取出执行的结果信息
                        String tempHandleMsg = XxlJobContext.getXxlJobContext().getHandleMsg();
                        //这里有一个三元运算，会判断执行结果信息是不是null，如果执行成功，毫无异常，这个结果信息就会是null
                        //只有在执行失败的时候，才会有失败信息被XxlJobHelper记录进去
                        tempHandleMsg = (tempHandleMsg != null && tempHandleMsg.length() > 50000)
                                ? tempHandleMsg.substring(0, 50000).concat("...")
                                : tempHandleMsg;
                        //这里是执行成功了，所以得到的是null，赋值其实就是什么也没赋成
                        XxlJobContext.getXxlJobContext().setHandleMsg(tempHandleMsg);
                    }

                    //走到这里，不管是执行成功还是失败，都要把结果存储到对应的日志文件中
                    //走到这里大家也应该意识到了，执行器这一端执行的定时任务，实际上是每一个定时任务都会对应一个本地的日志文件，每个定时任务的执行结果都会存储在自己的文件中
                    //当然，一个定时任务可能会执行很多次，所以定时任务对应的日志文件就会记录这个定时任务每次执行的信息
                    XxlJobHelper.log("<br>----------- xxl-job job execute end(finish) -----------<br>----------- Result: handleCode="
                                     + XxlJobContext.getXxlJobContext().getHandleCode()
                                     + ", handleMsg = "
                                     + XxlJobContext.getXxlJobContext().getHandleMsg()
                    );


                } else {
                    //走到这里说明触发器队列中没有数据，也就意味着没有要执行的定时任务
                    //如果线程的空闲时间大于30次，这里指的是循环的次数，每循环一次空闲时间就自增1，
                    //有定时任务被执行空闲时间就清零，不可能没任务线程空转，太浪费资源了
                    if (idleTimes > 30) {
                        //而且触发器队列也没有数据
                        if (triggerQueue.size() == 0) {
                            //就从缓存JobThread线程的jobThreadRepository这个Map中移除缓存的JobThread线程
                            //在移除的时候，会调用该线程的toStop方法和interrupt方法，让线程真正停下来
                            XxlJobExecutor.removeJobThread(jobId, "excutor idel times over limit.");
                        }
                    }
                }
            } catch (Throwable e) {
                if (toStop) {
                    //如果线程停止了，就记录线程停止的日志到定时任务对应的日志文件中
                    XxlJobHelper.log("<br>----------- JobThread toStop, stopReason:" + stopReason);
                    //下面就是将异常信息记录到日志文件中的操作，因为这些都是在catch中执行的
                    //就意味着肯定有异常了，所以要记录异常信息
                    StringWriter stringWriter = new StringWriter();
                    e.printStackTrace(new PrintWriter(stringWriter));
                    String errorMsg = stringWriter.toString();
                    XxlJobHelper.handleFail(errorMsg);
                    //在这里记录异常信息到日志文件中
                    XxlJobHelper.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- xxl-job job execute end(error) -----------");
                }
            } finally {
                //这里就走到了finally中，也就要开始执行日志回调给调度中心的操作了
                //别忘了，调度中心在远程调用之前创建了XxlJobLog这个对象，这个对象要记录很多日记调用信息的
                if (triggerParam != null) {
                    if (!toStop) {
                        //这里要再次判断线程是否停止运行
                        //如果没有停止，就创建封装回调信息的HandleCallbackParam对象
                        //把这个对象提交给TriggerCallbackThread内部的callBackQueue队列中
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                                // 日志id
                                triggerParam.getLogId(),
                                // 触发时间就是jobLog刚才设置的那个时间
                                triggerParam.getLogDateTime(),
                                // code
                                XxlJobContext.getXxlJobContext().getHandleCode(),
                                // 消息
                                XxlJobContext.getXxlJobContext().getHandleMsg())
                        );
                    } else {
                        //如果走到这里说明线程被终止了，就要封装处理失败的回信
                        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                                triggerParam.getLogId(),
                                triggerParam.getLogDateTime(),
                                //失败
                                XxlJobContext.HANDLE_CODE_FAIL,
                                stopReason + " [job running, killed]")
                        );
                    }
                }
            }
        }
        //代码走到这里就意味着退出了线程工作的while循环，虽然线程还未完全执行完run方法，但是已经意味着线程要停止了
        while (triggerQueue != null && triggerQueue.size() > 0) {
            //不为空就取出一个触发器参数
            TriggerParam triggerParam = triggerQueue.poll();
            if (triggerParam != null) {
                //下面就是封装回调信息，把执行结果回调给调度中心
                //这里的意思很简单，因为线程已经终止了，但是调用的定时任务还有没执行完的，要告诉调度中心
                TriggerCallbackThread.pushCallBack(new HandleCallbackParam(
                        triggerParam.getLogId(),
                        triggerParam.getLogDateTime(),
                        // 失败
                        XxlJobContext.HANDLE_CODE_FAIL,
                        stopReason + " [job not executed, in the job queue, killed.]")
                );
            }
        }

        try {
            //执行bean对象的销毁方法
            handler.destroy();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        logger.info(">>>>>>>>>>> xxl-job JobThread stoped, hashCode:{}", Thread.currentThread());
    }
}
