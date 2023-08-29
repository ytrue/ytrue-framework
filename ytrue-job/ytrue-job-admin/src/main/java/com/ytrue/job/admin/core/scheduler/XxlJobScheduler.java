package com.ytrue.job.admin.core.scheduler;

import com.ytrue.job.admin.core.conf.XxlJobAdminConfig;
import com.ytrue.job.admin.core.thread.JobRegistryHelper;
import com.ytrue.job.admin.core.thread.JobScheduleHelper;
import com.ytrue.job.admin.core.thread.JobTriggerPoolHelper;
import com.ytrue.job.core.biz.ExecutorBiz;
import com.ytrue.job.core.biz.client.ExecutorBizClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author ytrue
 * @date 2023-08-29 9:46
 * @description XxlJobScheduler
 */
public class XxlJobScheduler {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobScheduler.class);


    public void init() throws Exception {
        //初始化触发器线程池，这里面会创建两个线程池，一个快线程池，一个慢线程池
        //触发器任务的执行，就是由这两个线程池执行的
        JobTriggerPoolHelper.toStart();

        //初始化注册中心组件，这里是个简易版本，后面会重构到和源码一致
        //现在的版本并没有定时清理过期服务实例的功能
        JobRegistryHelper.getInstance().start();

        //初始化任务调度线程，这个线程可以说是xxl-job服务端的核心了
        //注意，大家在理解任务调度的时候，没必要把这个概念搞得特别复杂，所谓调度，就是哪个任务该执行了
        //这个线程就会把该任务提交了去执行，这就是调度的含义，这个线程会一直扫描判断哪些任务应该执行了
        //这里面会用到时间轮。这里我要再次强调一下，时间轮并不是线程，时间轮本身需要一个配合线程工作的容器
        //如果学过我的从零带你学Netty这门课，就会明白，时间轮的容器，可以用数组实现，也可以用Map实现
        //说得更准确点，容器加上工作线程组成了时间轮
        JobScheduleHelper.getInstance().start();
    }


    /**
     * 释放资源的方法
     *
     * @throws Exception
     */
    public void destroy() throws Exception {
        JobScheduleHelper.getInstance().toStop();
        JobRegistryHelper.getInstance().toStop();
        JobTriggerPoolHelper.toStop();
    }


    /**
     * 这个就是远程调用的Map集合，这个集合中，存储的就是专门用来远程调用的客户端
     * 这里的key是远程调用的服务实例的地址，value就是对应的客户端
     * 这里大家也应该意识到，在xxl-job中，进行远程调用，实际上使用的还是http，即使是在执行器那一端
     * 使用的也是Netty构建的http服务器
     */
    private static ConcurrentMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<>();


    /**
     * 通过这个方法可以获得一个进行远程调用的客户端。我想再次强调一下，所谓的客户端和服务端都是相对的
     * 当然，真正的服务端并发压力会大很多，但是仅从收发消息的角度来说，客户端和服务端都可以收发消息
     *
     * @param address
     * @return
     * @throws Exception
     */
    public static ExecutorBiz getExecutorBiz(String address) throws Exception {
        //判断远程地址是否为空
        if (address == null || address.trim().length() == 0) {
            return null;
        }
        //规整一下地址，去掉空格
        address = address.trim();
        //从远程调用的Map集合中获得远程调用的客户端
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null) {
            //如果有就直接返回
            return executorBiz;
        }
        //如果没有就创建一个客户端，然后存放到Map中，我现在是根据最新版本的源码来迭代手写代码的
        //但是，在旧版本，也就是2.0.2版本之前的版本，在xxl-job客户端，也就是执行器实例中，是用jetty进行通信的
        //在2.0.2版本之后，将jetty改成了netty，这个大家了解一下即可
        //这时候，本来作为客户端的执行器，在使用Netty构建了服务端后，又拥有服务端的身份了
        executorBiz = new ExecutorBizClient(address, XxlJobAdminConfig.getAdminConfig().getAccessToken());
        //把创建好的客户端放到Map中
        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }
}
