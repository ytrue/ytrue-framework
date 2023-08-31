package com.ytrue.job.admin.core.thread;

import com.ytrue.job.admin.core.conf.XxlJobAdminConfig;
import com.ytrue.job.admin.core.model.XxlJobGroup;
import com.ytrue.job.admin.core.model.XxlJobRegistry;
import com.ytrue.job.core.biz.model.RegistryParam;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.enums.RegistryConfig;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;
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


    //该线程的作用就是检测注册中心过期的执行器的
    private Thread registryMonitorThread;


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


        // 该线程就是用来循环检测注册中心注册的执行器是否过期，如果过期就移除过期数据，说白了，就是起到一个
        // 心跳检测的作用，该线程每次循环都会睡30秒，其实就是30秒检测一次过期的执行器
        registryMonitorThread = new Thread(() -> {
            while (!toStop) {
                try {

                    //这里查询的是所有自动注册的执行器组，手动录入的执行器不在次查询范围内，所谓自动注册，就是执行器启动时，通过http，把注册信息发送到
                    //调度中心的注册方式，并不是用户在web界面手动录入的注册方式
                    //注意，这里查询的是执行器组，还不是单个的执行器，也许现在大家还不明白是什么意思，往下看就会清楚了
                    List<XxlJobGroup> groupList = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().findByAddressType(0);

                    if (groupList != null && !groupList.isEmpty()) {
                        //这里的逻辑其实还要去对应的Mapper中查看，这里我把Mapper中的sql语句截取关键部分贴出来
                        //WHERE t.update_time <![CDATA[ < ]]> DATE_ADD(#{nowTime},INTERVAL -#{timeout} SECOND)
                        //其实就是判断数据库中记录的所有执行器的最新一次的更新时间是否小于当前时间减去90秒，这就意味着执行器的超时时间
                        //就是90秒，只要90秒内，执行器没有再更新自己的信息，就意味着它停机了
                        //而在执行器那一端，是每30秒就重新注册一次到注册中心
                        //注意，这里并没有区分是手动注册还是自动注册，只要是超时了的执行器都检测出来，然后从数据库中删除即可
                        List<Integer> ids = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().findDead(RegistryConfig.DEAD_TIMEOUT, new Date());

                        if (ids != null && ids.size() > 0) {
                            //上面的到了所有过期执行器的id集合，这里就直接删除过期的执行器
                            XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().removeDead(ids);
                        }

                        //该Map是用来缓存appName和对应的执行器地址的
                        HashMap<String, List<String>> appAddressMap = new HashMap<>();
                        //这里查处的就是所有没有过期的执行器，同样不用考虑注册类型，是否自动注册或手动录入，对应的sql语句如下
                        //WHERE t.update_time <![CDATA[ > ]]> DATE_ADD(#{nowTime},INTERVAL -#{timeout} SECOND)
                        //就是把小于号改成了大于号而已
                        List<XxlJobRegistry> list = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().findAll(RegistryConfig.DEAD_TIMEOUT, new Date());

                        if (list != null) {
                            //走到这里说明数据库中存在没有超时的执行器数据
                            for (XxlJobRegistry item: list) {
                                //遍历这些执行器
                                //先判断执行器是不是自动注册的，
                                if (RegistryConfig.RegistryType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                                    //如果是自动注册，就先获得执行器的项目名称，就是那个appName
                                    String appname = item.getRegistryKey();
                                    //以appName为key，判断刚才的Map中是否缓存着该appName对应的执行器地址
                                    List<String> registryList = appAddressMap.get(appname);
                                    if (registryList == null) {
                                        //如果没有则创建一个集合
                                        //这里之所以是集合，还是考虑到定时任务很可能部署在多个执行器上，而相同定时任务
                                        //的执行器名称是相同，正好可以用来当作key，value就是不同的执行器地址
                                        registryList = new ArrayList<String>();
                                    }
                                    //如果创建的这个集合尚未包含当前循环的执行器的地址
                                    if (!registryList.contains(item.getRegistryValue())) {
                                        //就把该地址存放到集合中
                                        registryList.add(item.getRegistryValue());
                                    }
                                    //把集合添加到Map中，至此，一个appName对应的执行器地址，这样的数据就通过键值对缓存成功了
                                    appAddressMap.put(appname, registryList);
                                }
                            }
                        }

                        //到这里会遍历最开始查询出来的自动注册的所有执行器组，注意，这时候，在上面的那个循环中，已经把
                        //所有未过期的执行器的信息用键值对的方式缓存在Map中了
                        for (XxlJobGroup group: groupList) {
                            //根据这个执行器注册到注册中心时记录的appName
                            //从Map中查询到所有的执行器地址，是个集合
                            List<String> registryList = appAddressMap.get(group.getAppname());
                            String addressListStr = null;
                            //判空操作
                            if (registryList!=null && !registryList.isEmpty()) {
                                //如果该执行器地址集合不为空，就把地址排一下序
                                //这里排序有什么意义呢？我没想出来。。排不排都无所谓吧，反正会有路由策略帮我们选择执行器地址
                                Collections.sort(registryList);
                                //开始把这些地址拼接到一块
                                StringBuilder addressListSB = new StringBuilder();
                                for (String item:registryList) {
                                    addressListSB.append(item).append(",");
                                }
                                addressListStr = addressListSB.toString();
                                //去掉最后一个逗号
                                addressListStr = addressListStr.substring(0, addressListStr.length()-1);
                            }
                            //然后把最新的执行器地址存放到执行器组中
                            group.setAddressList(addressListStr);
                            //更新执行器组的更新时间
                            group.setUpdateTime(new Date());
                            //在数据库中落实执行器组的更新
                            //到这里，大家应该能意识到了，执行器把自己注册到调度中心，是通过XxlJobRegistry对象
                            //来封装注册信息的，会被记录到数据库中
                            //但是注册线程会在后台默默工作，把各个appName相同的执行器的地址整合到一起，用XxlJobGroup对象封装
                            //等待调度定时任务的时候，其实就是从XxlJobGroup对象中获得appName的所有执行器地址，然后根据路由策略去
                            //选择具体的执行器地址来远程调用，这就是和注册有关的所有逻辑了，到此，该类中的代码和源码也完全一致了
                            XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().update(group);
                        }

                    }
                } catch (Exception e) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                    }
                }
                try {
                    //线程在这里睡30秒，也就意味着检测周期为30秒
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
                    }
                }
            }
            logger.info(">>>>>>>>>>> xxl-job, job registry monitor thread stop");
        });
        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("xxl-job, admin JobRegistryMonitorHelper-registryMonitorThread");
        registryMonitorThread.start();
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
     *
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
