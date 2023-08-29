package com.ytrue.job.admin.core.thread;

import com.ytrue.job.admin.core.conf.XxlJobAdminConfig;
import com.ytrue.job.core.biz.model.RegistryParam;
import com.ytrue.job.core.biz.model.ReturnT;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author ytrue
 * @date 2023-08-29 9:48
 * @description 该组件会初始化和注册中心相关的线程，大家可以想一想，执行器要注册到服务端，这些工作肯定就需要专门
 * 的线程来工作。而当执行器注册成功之后，如果过了不久就掉线了，也就是心跳检测超时，结果服务器这边不知道，还持有者掉线的执行器的地址
 * 这样一来，远程调用肯定是无法成功的。所以定期检查并清理掉线执行器也需要专门的线程来处理
 * 这两个操作，就是本类的职责
 * 当前的版本我们只需要注册执行器的线程池就可以了，不需要监控执行器是否过期
 */
public class JobRegistryHelper {

    private static final Logger logger = LoggerFactory.getLogger(JobRegistryHelper.class);

    /**
     * 在这里创建本类对象
     * <p>
     * 通过getInstance方法把本类对象暴露出去
     */
    @Getter
    private static JobRegistryHelper instance = new JobRegistryHelper();

    /**
     * 这个线程池就是用来注册或者溢出执行器地址的
     */
    private ThreadPoolExecutor registryOrRemoveThreadPool = null;

    /**
     * 是否停止
     */
    private volatile boolean toStop = false;


    /**
     * 创建并启动上面的线程池
     */
    public void start() {
        //执行注册和移除执行器地址任务的线程池在这里被创建了
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
                // 核心线程数
                2,
                // 最大线程数
                10,
                // 活跃时间
                30L,
                TimeUnit.SECONDS,
                // 队列大小
                new LinkedBlockingQueue<>(2000),
                // 线程工厂
                r -> new Thread(r, "xxl-job, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode()),
                //下面这个是xxl-job定义的线程池拒绝策略，其实就是把被拒绝的任务再执行一遍
                (r, executor) -> {
                    //在这里能看到，所谓的拒绝，就是把任务再执行一遍
                    r.run();
                    logger.warn(">>>>>>>>>>> xxl-job, registry or remove too fast, match threadpool rejected handler(run now).");
                });
    }


    /**
     * 关闭线程池的方法
     */
    public void toStop() {
        toStop = true;
        registryOrRemoveThreadPool.shutdownNow();
    }


    /**
     * 注册执行器的方法,其实就是向数据库保存这条记录
     *
     * @param registryParam
     * @return
     */
    public ReturnT<String> registry(RegistryParam registryParam) {
        //校验处理
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
            || !StringUtils.hasText(registryParam.getRegistryKey())
            || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }
        //提交注册执行器的任务给线程池执行
        registryOrRemoveThreadPool.execute(() -> {
            //这里的意思也很简单，就是先根据registryParam参数去数据库中更新相应的数据
            //如果返回的是0，说明数据库中没有相应的信息，该执行器还没注册到注册中心呢，所以下面
            //就可以直接新增这一条数据即可
            //  UPDATE xxl_job_registry
            //        SET `update_time` = #{updateTime}
            //        WHERE `registry_group` = #{registryGroup}
            //          AND `registry_key` = #{registryKey}
            //          AND `registry_value` = #{registryValue}
            int ret = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registryUpdate(
                    registryParam.getRegistryGroup(),
                    registryParam.getRegistryKey(),
                    registryParam.getRegistryValue(),
                    new Date()
            );
            if (ret < 1) {
                //这里就是数据库中没有相应数据，直接新增即可
                //  INSERT INTO xxl_job_registry( `registry_group` , `registry_key` , `registry_value`, `update_time`)
                //        VALUES( #{registryGroup}  , #{registryKey} , #{registryValue}, #{updateTime})
                XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registrySave(
                        registryParam.getRegistryGroup(),
                        registryParam.getRegistryKey(),
                        registryParam.getRegistryValue(),
                        new Date()
                );
                //该方法从名字上看是刷新注册表信息的意思
                //但是作者还没有实现，源码中就是空的，所以这里我就照搬过来了
                freshGroupRegistryInfo(registryParam);
            }
        });
        return ReturnT.SUCCESS;
    }


    /**
     * 移除过期的执行器地址
     * @param registryParam
     * @return
     */
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        //校验处理
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
            || !StringUtils.hasText(registryParam.getRegistryKey())
            || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }
        //将任务提交给线程池来处理
        registryOrRemoveThreadPool.execute(() -> {
            //在这里直接根据registryParam从数据库中删除对应的执行器地址
            //这里的返回结果是删除了几条数据的意思
            // DELETE FROM xxl_job_registry
            //		WHERE registry_group = #{registryGroup}
            //			AND registry_key = #{registryKey}
            //			AND registry_value = #{registryValue}
            int ret = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().registryDelete(
                    registryParam.getRegistryGroup(),
                    registryParam.getRegistryKey(),
                    registryParam.getRegistryValue()
            );
            if (ret > 0) {
                //上个方法已经讲过了，这里就不再讲了
                freshGroupRegistryInfo(registryParam);
            }
        });
        return ReturnT.SUCCESS;
    }



    /**
     * 这个方法在源码中就是空的。。作者也没想好要怎么弄呢。
     *
     * @param registryParam
     */
    private void freshGroupRegistryInfo(RegistryParam registryParam) {
    }


}
