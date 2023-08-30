package com.ytrue.job.admin.core.trigger;

import com.ytrue.job.admin.core.conf.XxlJobAdminConfig;
import com.ytrue.job.admin.core.model.XxlJobGroup;
import com.ytrue.job.admin.core.model.XxlJobInfo;
import com.ytrue.job.admin.core.scheduler.XxlJobScheduler;
import com.ytrue.job.admin.core.util.I18nUtil;
import com.ytrue.job.core.biz.ExecutorBiz;
import com.ytrue.job.core.biz.model.ReturnT;
import com.ytrue.job.core.biz.model.TriggerParam;
import com.ytrue.job.core.util.ThrowableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-29 16:00
 * @description 该类也是xxl-job中很重要的一个类，job的远程调用就是在该类中进行的，当然不是直接进行，远程调用
 * 到最后，任务还是在执行器那端执行，但是该类会为远程调用做很多必要的辅助性工作，比如选择路由策略，然后选择要执行
 * 任务的执行器的地址。现在这些功能我们都还没有引入，只是用最简单的，选择第一个服务实例地址进行远程调用
 */
public class XxlJobTrigger {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobTrigger.class);


    /**
     * 该方法是远程调用前的准备阶段，在该方法内，如果用户自己设置了执行器的地址和执行器的任务参数，
     * 以及分片策略，在该方法内会对这些操作进行处理
     *
     * @param jobId
     * @param triggerType
     * @param failRetryCount
     * @param executorShardingParam
     * @param executorParam
     * @param addressList
     */
    public static void trigger(int jobId,
                               TriggerTypeEnum triggerType,
                               int failRetryCount,
                               String executorShardingParam,
                               String executorParam,
                               String addressList) {

        //根据任务id，从数据库中查询到该任务的完整信息
        XxlJobInfo jobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(jobId);

        //如果任务为null，则打印一条告警信息
        if (jobInfo == null) {
            logger.warn(">>>>>>>>>>>> trigger fail, jobId invalid，jobId={}", jobId);
            return;
        }
        //如果用户在页面选择执行任务的时候，传递参数进来了，这时候就把任务参数设置到job中
        if (executorParam != null) {
            //设置执行器的任务参数
            jobInfo.setExecutorParam(executorParam);
        }
        //同样是根据jobId获取所有的执行器组
        // 	SELECT <include refid="Base_Column_List" />
        //		FROM xxl_job_group AS t
        //		WHERE t.id = #{id}
        XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(jobInfo.getJobGroup());

        //这里也有一个小判断，如果用户在web界面输入了执行器的地址，这里会把执行器的地址设置到刚才查询到的执行器中
        //注意，这里我想强调两点，第一，这里以及上面那个设置执行器参数，都是在web界面对任务进行执行一次操作时，才会出现的调用流程
        //这个大家要弄清楚
        //第二点要强调的就是，XxlJobGroup这个对象，它并不是说里面有集合还是还是什么，在真正的生产环境中，一个定时任务不可能
        //只有一个服务器在执行吧，显然会有多个服务器在执行，对于相同的定时任务，注册到XXL-JOB的服务器上时，会把相同定时任务
        //的服务实例地址规整到一起，就赋值给XxlJobGroup这个类的addressList成员变量，不同的地址用逗号分隔即可
        if (addressList != null && addressList.trim().length() > 0) {
            //这里是设置执行器地址的注册方式，0是自动注册，就是1是用户手动注册的
            group.setAddressType(1);
            group.setAddressList(addressList.trim());
        }
        //执行触发器任务，这里有几个参数我直接写死了，因为现在还用不到，为了不报错，我们就直接写死
        //这里写死的都是沿用源码中设定的默认值
        //其实这里的index和total参数分别代表分片序号和分片总数的意思，但现在我们没有引入分片，只有一台执行器
        //执行定时任务，所以分片序号为0，分片总是为1。
        //分片序号代表的是执行器，如果有三个执行器，那分片序号就是0，1，2
        //分片总数就为3，这里虽然有这两个参数，实际上在第一个版本还用不到。之所以不把参数略去是因为，这样一来
        //需要改动的地方就有点多了，大家理解一下
        //在该方法内，会真正开始远程调用，这个方法，也是远程调用的核心方法
        processTrigger(group, jobInfo, -1, triggerType, 0, 1);
    }


    /**
     * 在该方法中会进一步处理分片和路由策略
     *
     * @param group
     * @param jobInfo
     * @param finalFailRetryCount
     * @param triggerType
     * @param index
     * @param total
     */
    private static void processTrigger(XxlJobGroup group, XxlJobInfo jobInfo, int finalFailRetryCount, TriggerTypeEnum triggerType, int index, int total) {
        //初始化触发器参数，这里的这个触发器参数，是要在远程调用的另一端，也就是执行器那一端使用的
        TriggerParam triggerParam = new TriggerParam();
        //设置任务id
        triggerParam.setJobId(jobInfo.getId());
        //设置执行器要执行的任务的方法名称
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        //把执行器要执行的任务的参数设置进去
        triggerParam.setExecutorParams(jobInfo.getExecutorParam());
        //设置执行模式，一般都是bean模式
        triggerParam.setGlueType(jobInfo.getGlueType());
        //接下来要再次设定远程调用的服务实例的地址
        //这里其实是考虑到了路由策略，但是第一版本还不涉及这些知识，所以就先不这么做了
        String address = null;
        //得到所有注册到服务端的执行器的地址，并且做判空处理
        List<String> registryList = group.getRegistryList();
        if (registryList != null && !registryList.isEmpty()) {
            //在源码中，本来这里就要使用路由策略，选择具体的执行器地址了，但是现在我们还没有引入路由策略
            //所以这里就简单处理，就使用集合中的第一个地址
            address = registryList.get(0);
        }
        //接下来就定义一个远程调用的结果变量
        ReturnT<String> triggerResult;
        //如果地址不为空
        if (address != null) {
            //在这里进行远程调用，这里就是最核心远程调用的方法，但是方法内部的逻辑很简单，就是用http发送调用
            //消息而已
            triggerResult = runExecutor(triggerParam, address);
            //这里就输出一下状态码吧，根据返回的状态码判断任务是否执行成功
            logger.info("返回的状态码" + triggerResult.getCode());
        } else {
            logger.warn("执行器地址为空");
            triggerResult = new ReturnT<String>(ReturnT.FAIL_CODE, null);
        }
    }


    /**
     * 该方法内进行远程调用
     *
     * @param triggerParam
     * @param address
     * @return
     */
    public static ReturnT<String> runExecutor(TriggerParam triggerParam, String address) {
        ReturnT<String> runResult;
        try {
            //获取一个用于远程调用的客户端对象，一个地址就对应着一个客户端，为什么说是客户端，因为远程调用的时候，执行器
            //就成为了服务端，因为执行器要接收来自客户端的调用消息
            ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
            //客户端获得之后，就在run方法内进行远程调用了
            runResult = executorBiz.run(triggerParam);
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> xxl-job trigger error, please check if the executor[{}] is running.", address, e);
            runResult = new ReturnT<>(ReturnT.FAIL_CODE, ThrowableUtil.toString(e));
        }
        //在这里拼接一下远程调用返回的状态码和消息
        StringBuffer runResultSB = new StringBuffer(I18nUtil.getString("jobconf_trigger_run") + "：");
        runResultSB.append("<br>address：").append(address);
        runResultSB.append("<br>code：").append(runResult.getCode());
        runResultSB.append("<br>msg：").append(runResult.getMsg());
        runResult.setMsg(runResultSB.toString());
        return runResult;
    }

}
