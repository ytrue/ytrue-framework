package com.ytrue.job.core.thread;

import com.ytrue.job.core.biz.AdminBiz;
import com.ytrue.job.core.biz.model.RegistryParam;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.enums.RegistryConfig;
import com.ytrue.job.core.executor.XxlJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-08-28 14:06
 * @description ExecutorRegistryThread
 */
public class ExecutorRegistryThread {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorRegistryThread.class);

    /**
     * 创建该类的对象
     */
    private static ExecutorRegistryThread instance = new ExecutorRegistryThread();

    /**
     * 通过该方法将该类对象暴露出去
     *
     * @return
     */
    public static ExecutorRegistryThread getInstance() {
        return instance;
    }

    /**
     * 将执行器注册到调度中心的线程，也就是真正干活的线程
     */
    private Thread registryThread;
    /**
     * 线程是否停止工作的标记
     */
    private volatile boolean toStop = false;


    /**
     * 启动注册线程
     *
     * @param appName
     * @param address
     */
    public void start(final String appName, final String address) {
        //对appname判空，这个就是执行器要记录在调度衷心的名称
        if (appName == null || appName.trim().length() == 0) {
            logger.warn(">>>>>>>>>>> xxl-job, executor registry config fail, appname is null.");
            return;
        }
        //判断adminBizList集合不为空，因为个客户端是用来和调度中心通信的
        if (XxlJobExecutor.getAdminBizList() == null) {
            logger.warn(">>>>>>>>>>> xxl-job, executor registry config fail, adminAddresses is null.");
            return;
        }

        //创建线程
        registryThread = new Thread(() -> {
            //在一个循环中执行注册任务
            while (!toStop) {
                try {
                    //根据appname和address创建注册参数，注意，这里的address是执行器的地址，只有一个，别和调度中心的地址搞混了
                    RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistryType.EXECUTOR.name(), appName, address);
                    //这里考虑到调度中心也许是以集群的形式存在，所以从集合中得到每一个和调度中心通话地客户端，然后发送注册消息即可
                    for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                        try {
                            //在这里执行注册
                            ReturnT<String> registryResult = adminBiz.registry(registryParam);
                            if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                                registryResult = ReturnT.SUCCESS;
                                logger.debug(">>>>>>>>>>> xxl-job registry success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                                //注册成功则打破循环，因为注册成功一个后，调度中心就把相应的数据写到数据库中了，没必要每个都注册
                                //直接退出循环即可
                                //注册不成功，再找下一个注册中心继续注册
                                break;
                            } else {
                                //如果注册失败了，就寻找下一个调度中心继续注册
                                logger.info(">>>>>>>>>>> xxl-job registry fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                            }
                        } catch (Exception e) {
                            logger.info(">>>>>>>>>>> xxl-job registry error, registryParam:{}", registryParam, e);
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                try {
                    if (!toStop) {
                        //这里是每隔30秒，就再次循环重新注册一次，也就是维持心跳信息。
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    }
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.warn(">>>>>>>>>>> xxl-job, executor registry thread interrupted, error msg:{}", e.getMessage());
                    }
                }
            }


            //----------------------------------------------
            try {
                //这里要注意，当程序执行到这里的时候，就意味着跳出了上面那个工作线程的循环，其实也就意味着那个工作线程要结束工作了，不再注册执行器，也不再刷新心跳信息
                //这也就意味着执行器这一端可能不再继续提供服务了，所以下面要把注册的执行器信息从调度中心删除，所以发送删除的信息给调度中心
                //再次创建注册参数对象
                RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistryType.EXECUTOR.name(), appName, address);
                for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                    try {
                        //在这里发送删除执行器的信息
                        ReturnT<String> registryResult = adminBiz.registryRemove(registryParam);
                        if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                            registryResult = ReturnT.SUCCESS;
                            logger.info(">>>>>>>>>>> xxl-job registry-remove success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                            break;
                        } else {
                            logger.info(">>>>>>>>>>> xxl-job registry-remove fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                        }
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.info(">>>>>>>>>>> xxl-job registry-remove error, registryParam:{}", registryParam, e);
                        }
                    }
                }
            } catch (Exception e) {
                if (!toStop) {
                    logger.error(e.getMessage(), e);
                }
            }
            logger.info(">>>>>>>>>>> xxl-job, executor registry thread destroy.");
        });
        //在这里启动线程
        registryThread.setDaemon(true);
        registryThread.setName("xxl-job, executor ExecutorRegistryThread");
        registryThread.start();
    }


    /**
     * 终止注册线程的方法
     */
    public void toStop() {
        //改变线程是否停止的标记
        toStop = true;
        if (registryThread != null) {
            //中断注册线程
            registryThread.interrupt();
            try {
                //这里就是线程的最基础的知识，在在哪个线程中调用了注册线程的join方法
                //哪个线程就会暂时阻塞住，等待注册线程执行完了才会继续向下执行
                registryThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

}
