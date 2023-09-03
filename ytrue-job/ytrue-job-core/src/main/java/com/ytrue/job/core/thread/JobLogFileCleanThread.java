package com.ytrue.job.core.thread;

import com.ytrue.job.core.log.XxlJobFileAppender;
import com.ytrue.job.core.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-09-03 14:09
 * @description JobLogFileCleanThread 执行器日志文件清理
 */
public class JobLogFileCleanThread {

    private static Logger logger = LoggerFactory.getLogger(JobLogFileCleanThread.class);

    /**
     * 创建单例对象
     */
    private static JobLogFileCleanThread instance = new JobLogFileCleanThread();

    /**
     * 把对象暴露出去
     *
     * @return
     */
    public static JobLogFileCleanThread getInstance() {
        return instance;
    }

    /**
     * 工作线程
     */
    private Thread localThread;
    /**
     * 判断线程是否停止工作
     */
    private volatile boolean toStop = false;


    /**
     * 启动该组件的方法
     *
     * @param logRetentionDays
     */
    public void start(final long logRetentionDays) {
        //logRetentionDays为用户在配置文件设定的日志过期时间
        //这里有个判断，如果日志过期时间少于3天就直接退出
        if (logRetentionDays < 3) {
            return;
        }
        localThread = new Thread(() -> {
            while (!toStop) {
                try {
                    //得到该路径下的所有日志文件
                    File[] childDirs = new File(XxlJobFileAppender.getLogPath()).listFiles();
                    if (childDirs != null && childDirs.length > 0) {
                        //判断日志文件数组非空
                        //得到当前时间
                        Calendar todayCal = Calendar.getInstance();
                        //设置日期
                        todayCal.set(Calendar.HOUR_OF_DAY, 0);
                        //设置分钟
                        todayCal.set(Calendar.MINUTE, 0);
                        //设置秒
                        todayCal.set(Calendar.SECOND, 0);
                        //设置毫秒
                        todayCal.set(Calendar.MILLISECOND, 0);
                        //得到零点时间
                        Date todayDate = todayCal.getTime();

                        //遍历日志文件
                        for (File childFile : childDirs) {
                            // 如果不是文件夹就跳过这次循环，因为现在找到的都是文件夹，文件夹的名称是定时任务执行的年月日时间
                            // 比如，2023-06-30，2023-07-02等等，每个时间都是一个文件见，文件夹中有很多个日志文件，文件名称就是定时任务的ID
                            if (!childFile.isDirectory()) {
                                continue;
                            }
                            //判断文件夹中是否有-符号，根据我上面举的例子，显然有文件夹的名称中有-符号
                            //如果没有则跳过这个文件夹
                            if (childFile.getName().indexOf("-") == -1) {
                                continue;
                            }
                            //该变量就用来记录日志文件的创建时间，其实就是文件夹的名字
                            Date logFileCreateDate = null;
                            try {
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                                //得到创建时间
                                logFileCreateDate = simpleDateFormat.parse(childFile.getName());
                            } catch (ParseException e) {
                                logger.error(e.getMessage(), e);
                            }
                            if (logFileCreateDate == null) {
                                continue;
                            }
                            //计算刚才得到的今天的零点时间减去日志文件创建的时间是否大于了用户设定的日志过期时间
                            if ((todayDate.getTime() - logFileCreateDate.getTime()) >= logRetentionDays * (24 * 60 * 60 * 1000)) {
                                //如果超过了就把过期的日志删除了
                                FileUtil.deleteRecursively(childFile);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }

                }
                try {
                    TimeUnit.DAYS.sleep(1);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> xxl-job, executor JobLogFileCleanThread thread destroy.");

        });
        localThread.setDaemon(true);
        localThread.setName("xxl-job, executor JobLogFileCleanThread");
        localThread.start();
    }


    /**
     * 终止组件运行的方法
     */
    public void toStop() {
        toStop = true;
        if (localThread == null) {
            return;
        }
        localThread.interrupt();
        try {
            localThread.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
